package com.smartit.smartsales.service;

import com.smartit.smartsales.domain.Commercial;
import com.smartit.smartsales.domain.Visite;
import com.smartit.smartsales.domain.enums.Role;
import com.smartit.smartsales.domain.enums.StatutVisite;
import com.smartit.smartsales.dto.request.CheckRequest;
import com.smartit.smartsales.dto.request.VisitePatchRequest;
import com.smartit.smartsales.dto.request.VisiteRequest;
import com.smartit.smartsales.dto.response.VisiteResponse;
import com.smartit.smartsales.exception.ResourceNotFoundException;
import com.smartit.smartsales.repository.CommercialRepository;
import com.smartit.smartsales.repository.UserRepository;
import com.smartit.smartsales.repository.VisiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VisiteService {

    private final VisiteRepository visiteRepository;
    private final CommercialRepository commercialRepository;
    private final ClientService clientService;
    private final UserRepository userRepository;

    public List<VisiteResponse> findAll() {
        String username = currentUsername();
        if (isCommercial(username)) {
            Long commercialId = getCommercialId(username);
            return visiteRepository.findByCommercialId(commercialId).stream().map(this::toResponse).toList();
        }
        return visiteRepository.findAll().stream().map(this::toResponse).toList();
    }

    public VisiteResponse findById(Long id) {
        Visite visite = get(id);
        checkAccess(visite);
        return toResponse(visite);
    }

    public VisiteResponse create(VisiteRequest request) {
        Commercial commercial = null;
        if (request.commercialId() != null) {
            commercial = commercialRepository.findById(request.commercialId())
                    .orElseThrow(() -> new ResourceNotFoundException("Commercial introuvable : " + request.commercialId()));
        }
        Visite visite = Visite.builder()
                .commercial(commercial)
                .client(clientService.get(request.clientId()))
                .dateVisite(request.dateVisite())
                .type(request.type())
                .statut(request.statut() != null ? request.statut() : com.smartit.smartsales.domain.enums.StatutVisite.PLANIFIEE)
                .compteRendu(request.compteRendu())
                .ordreTournee(request.ordreTournee())
                .montant(request.montant() != null ? request.montant() : java.math.BigDecimal.ZERO)
                .build();
        return toResponse(visiteRepository.save(visite));
    }

    public VisiteResponse update(Long id, VisiteRequest request) {
        Visite visite = get(id);
        checkAccess(visite);
        visite.setDateVisite(request.dateVisite());
        visite.setType(request.type());
        if (request.statut() != null) visite.setStatut(request.statut());
        visite.setCompteRendu(request.compteRendu());
        visite.setOrdreTournee(request.ordreTournee());
        if (request.montant() != null) visite.setMontant(request.montant());
        return toResponse(visiteRepository.save(visite));
    }

    public VisiteResponse patch(Long id, VisitePatchRequest request) {
        Visite visite = get(id);
        checkAccess(visite);
        if (request.statut() != null) visite.setStatut(request.statut());
        visite.setCompteRendu(request.compteRendu());
        if (request.montant() != null) visite.setMontant(request.montant());
        return toResponse(visiteRepository.save(visite));
    }

    public VisiteResponse checkIn(Long id, CheckRequest request) {
        Visite visite = get(id);
        checkAccess(visite);
        visite.setCheckIn(java.time.LocalDateTime.now());
        if (request != null && request.latitude()  != null) visite.setCheckInLatitude(request.latitude());
        if (request != null && request.longitude() != null) visite.setCheckInLongitude(request.longitude());
        // Passage automatique en EN_COURS si la visite était encore PLANIFIEE
        if (visite.getStatut() == StatutVisite.PLANIFIEE) {
            visite.setStatut(StatutVisite.EN_COURS);
        }
        return toResponse(visiteRepository.save(visite));
    }

    public VisiteResponse checkOut(Long id, CheckRequest request) {
        Visite visite = get(id);
        checkAccess(visite);
        visite.setCheckOut(java.time.LocalDateTime.now());
        if (request != null && request.latitude()  != null) visite.setCheckOutLatitude(request.latitude());
        if (request != null && request.longitude() != null) visite.setCheckOutLongitude(request.longitude());
        return toResponse(visiteRepository.save(visite));
    }

    public void delete(Long id) {
        visiteRepository.delete(get(id));
    }

    private Visite get(Long id) {
        return visiteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Visite introuvable : " + id));
    }

    private void checkAccess(Visite visite) {
        String username = currentUsername();
        if (isCommercial(username)) {
            if (visite.getCommercial() == null) {
                throw new AccessDeniedException("Accès refusé : visite non assignée");
            }
            Long myId = getCommercialId(username);
            if (!visite.getCommercial().getId().equals(myId)) {
                throw new AccessDeniedException("Accès refusé à cette visite");
            }
        }
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private boolean isCommercial(String username) {
        return userRepository.findByUsername(username)
                .map(u -> u.getRole() == Role.COMMERCIAL)
                .orElse(false);
    }

    private Long getCommercialId(String username) {
        return commercialRepository.findByUserUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Profil commercial non trouvé pour : " + username))
                .getId();
    }

    public VisiteResponse toResponse(Visite v) {
        var c = v.getClient();
        VisiteResponse.ClientInfo clientInfo = c != null
                ? new VisiteResponse.ClientInfo(c.getId(), c.getNom(), c.getAdresse(), c.getLatitude(), c.getLongitude())
                : null;
        return new VisiteResponse(
                v.getId(),
                v.getCommercial() != null ? v.getCommercial().getId() : null,
                v.getCommercial() != null ? v.getCommercial().getNom() + " " + v.getCommercial().getPrenom() : null,
                c != null ? c.getId() : null,
                c != null ? c.getNom() : null,
                clientInfo,
                v.getDateVisite(),
                v.getType(),
                v.getStatut(),
                v.getCompteRendu(),
                v.getOrdreTournee(),
                v.getCheckIn(),
                v.getCheckOut(),
                v.getCheckInLatitude(),
                v.getCheckInLongitude(),
                v.getCheckOutLatitude(),
                v.getCheckOutLongitude(),
                v.getMontant() != null ? v.getMontant() : java.math.BigDecimal.ZERO
        );
    }
}
