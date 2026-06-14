package com.smartit.smartsales.service;

import com.smartit.smartsales.domain.Commercial;
import com.smartit.smartsales.domain.enums.Role;
import com.smartit.smartsales.dto.ai.AnalyseCommercialInputDto;
import com.smartit.smartsales.dto.ai.AnalyseHistoriqueItemDto;
import com.smartit.smartsales.dto.ai.AnalyseRequestDto;
import com.smartit.smartsales.dto.ai.AnalyseResponseDto;
import com.smartit.smartsales.dto.ai.AnalyseResultatDto;
import com.smartit.smartsales.dto.response.PerformanceCalculeeResponse;
import com.smartit.smartsales.exception.ResourceNotFoundException;
import com.smartit.smartsales.repository.CommercialRepository;
import com.smartit.smartsales.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PerformanceAnalyseService {

    private final PerformanceCalculeeService performanceCalculeeService;
    private final AiAnalyseClient aiAnalyseClient;
    private final CommercialRepository commercialRepository;
    private final UserRepository userRepository;

    /** Analyse IA pour un commercial donné (MANAGER/ADMIN). */
    public AnalyseResultatDto analyserCommercial(Long commercialId) {
        Commercial commercial = commercialRepository.findById(commercialId)
                .orElseThrow(() -> new ResourceNotFoundException("Commercial introuvable : " + commercialId));
        return analyser(commercial);
    }

    /** Analyse IA du commercial connecté (COMMERCIAL). */
    public AnalyseResultatDto analyserPourMoi() {
        String username = currentUsername();
        if (!isCommercial(username)) throw new AccessDeniedException("Réservé aux commerciaux");
        Commercial commercial = commercialRepository.findByUserUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Profil commercial introuvable : " + username));
        return analyser(commercial);
    }

    private AnalyseResultatDto analyser(Commercial commercial) {
        // Récupère tout l'historique de performances calculées pour ce commercial
        List<PerformanceCalculeeResponse> historique = performanceCalculeeService.calculerHistorique(commercial);

        List<AnalyseHistoriqueItemDto> items = historique.stream()
                .map(p -> new AnalyseHistoriqueItemDto(
                        p.periode(),
                        p.chiffreAffaires(),
                        p.nombreVisites(),
                        p.nombreVisitesTerminees(),
                        p.tauxConversion()
                ))
                .toList();

        AnalyseCommercialInputDto input = new AnalyseCommercialInputDto(
                commercial.getId(),
                commercial.getNom() + " " + commercial.getPrenom(),
                items
        );

        AnalyseResponseDto response = aiAnalyseClient.analyser(new AnalyseRequestDto(List.of(input)));

        return response.analyses().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Aucune analyse retournée par le service IA"));
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private boolean isCommercial(String username) {
        return userRepository.findByUsername(username)
                .map(u -> u.getRole() == Role.COMMERCIAL)
                .orElse(false);
    }
}
