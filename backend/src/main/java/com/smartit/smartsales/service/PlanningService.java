package com.smartit.smartsales.service;

import com.smartit.smartsales.domain.Commercial;
import com.smartit.smartsales.domain.Visite;
import com.smartit.smartsales.domain.enums.Role;
import com.smartit.smartsales.domain.enums.StatutVisite;
import com.smartit.smartsales.domain.enums.TypeVisite;
import com.smartit.smartsales.dto.ai.OptimizeRequestDto;
import com.smartit.smartsales.dto.ai.OptimizeResponseDto;
import com.smartit.smartsales.dto.ai.VisitePointDto;
import com.smartit.smartsales.dto.response.PlanningResponse;
import com.smartit.smartsales.dto.response.VisiteResponse;
import com.smartit.smartsales.exception.ResourceNotFoundException;
import com.smartit.smartsales.repository.CommercialRepository;
import com.smartit.smartsales.repository.UserRepository;
import com.smartit.smartsales.repository.VisiteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanningService {

    private final VisiteRepository visiteRepository;
    private final CommercialRepository commercialRepository;
    private final UserRepository userRepository;
    private final AiOptimizerClient aiOptimizerClient;
    private final VisiteService visiteService;

    @Transactional
    public PlanningResponse optimiserTournee(Long commercialId, LocalDate date) {
        // Vérification des droits : un COMMERCIAL ne peut optimiser que son propre planning
        checkCommercialAccess(commercialId);

        Commercial commercial = commercialRepository.findById(commercialId)
                .orElseThrow(() -> new ResourceNotFoundException("Commercial introuvable : " + commercialId));

        // Récupération des visites PLANIFIEE pour ce commercial et cette date
        List<Visite> visites = visiteRepository.findByCommercialIdAndStatutAndDateVisiteBetween(
                commercialId,
                StatutVisite.PLANIFIEE,
                date.atStartOfDay(),
                date.atTime(LocalTime.MAX)
        );

        if (visites.isEmpty()) {
            return new PlanningResponse(List.of(), 0.0);
        }

        // Séparation : visites avec coordonnées / sans coordonnées
        List<Visite> avecCoords = visites.stream()
                .filter(v -> v.getClient().getLatitude() != null && v.getClient().getLongitude() != null)
                .toList();
        List<Visite> sansCoords = visites.stream()
                .filter(v -> v.getClient().getLatitude() == null || v.getClient().getLongitude() == null)
                .toList();

        if (avecCoords.isEmpty()) {
            // Aucune coordonnée disponible : on ne peut pas optimiser, on attribue un ordre naturel
            for (int i = 0; i < sansCoords.size(); i++) {
                sansCoords.get(i).setOrdreTournee(i + 1);
            }
            visiteRepository.saveAll(sansCoords);
            List<VisiteResponse> responses = sansCoords.stream().map(visiteService::toResponse).toList();
            return new PlanningResponse(responses, 0.0);
        }

        // Construction de la requête vers le service IA
        List<VisitePointDto> points = avecCoords.stream()
                .map(v -> new VisitePointDto(
                        v.getId().intValue(),
                        v.getClient().getNom(),
                        v.getClient().getLatitude(),
                        v.getClient().getLongitude(),
                        v.getType().name(),
                        prioriteDuType(v.getType())
                ))
                .toList();

        OptimizeRequestDto aiRequest = new OptimizeRequestDto(null, null, points);

        // Appel au service IA (lance ServiceUnavailableException si injoignable)
        OptimizeResponseDto aiResponse = aiOptimizerClient.optimize(aiRequest);

        // Mise à jour de l'ordre de tournée sur chaque visite
        Map<Long, Visite> visiteById = avecCoords.stream()
                .collect(Collectors.toMap(Visite::getId, Function.identity()));

        for (int i = 0; i < aiResponse.ordre().size(); i++) {
            Long visiteId = aiResponse.ordre().get(i).longValue();
            Visite v = visiteById.get(visiteId);
            if (v != null) {
                v.setOrdreTournee(i + 1);
            }
        }

        // Les visites sans coordonnées vont à la fin
        int offset = avecCoords.size();
        for (int i = 0; i < sansCoords.size(); i++) {
            sansCoords.get(i).setOrdreTournee(offset + i + 1);
        }

        visiteRepository.saveAll(avecCoords);
        visiteRepository.saveAll(sansCoords);

        // Réponse triée par ordreTournee
        List<VisiteResponse> sorted = visites.stream()
                .sorted(Comparator.comparingInt(v -> v.getOrdreTournee() != null ? v.getOrdreTournee() : Integer.MAX_VALUE))
                .map(visiteService::toResponse)
                .toList();

        return new PlanningResponse(sorted, aiResponse.distanceTotaleKm());
    }

    /** Priorité métier : NEGOCIATION > RELANCE > PROSPECTION. */
    private int prioriteDuType(TypeVisite type) {
        return switch (type) {
            case NEGOCIATION -> 3;
            case RELANCE -> 2;
            case PROSPECTION -> 1;
        };
    }

    private void checkCommercialAccess(Long commercialId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        boolean isCommercial = userRepository.findByUsername(username)
                .map(u -> u.getRole() == Role.COMMERCIAL)
                .orElse(false);

        if (isCommercial) {
            Long myCommercialId = commercialRepository.findByUserUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("Profil commercial introuvable"))
                    .getId();
            if (!myCommercialId.equals(commercialId)) {
                throw new AccessDeniedException("Un commercial ne peut optimiser que son propre planning");
            }
        }
    }
}
