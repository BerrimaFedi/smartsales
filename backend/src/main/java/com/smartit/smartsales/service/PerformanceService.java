package com.smartit.smartsales.service;

import com.smartit.smartsales.domain.Performance;
import com.smartit.smartsales.domain.enums.Role;
import com.smartit.smartsales.dto.request.PerformanceRequest;
import com.smartit.smartsales.dto.response.PerformanceResponse;
import com.smartit.smartsales.exception.ResourceNotFoundException;
import com.smartit.smartsales.repository.CommercialRepository;
import com.smartit.smartsales.repository.PerformanceRepository;
import com.smartit.smartsales.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PerformanceService {

    private final PerformanceRepository performanceRepository;
    private final CommercialRepository commercialRepository;
    private final UserRepository userRepository;

    public List<PerformanceResponse> findAll() {
        String username = currentUsername();
        if (isCommercial(username)) {
            Long commercialId = getCommercialId(username);
            return performanceRepository.findByCommercialId(commercialId).stream().map(this::toResponse).toList();
        }
        return performanceRepository.findAll().stream().map(this::toResponse).toList();
    }

    public PerformanceResponse findById(Long id) {
        Performance p = get(id);
        String username = currentUsername();
        if (isCommercial(username)) {
            Long myCommercialId = getCommercialId(username);
            if (!p.getCommercial().getId().equals(myCommercialId)) {
                throw new AccessDeniedException("Accès refusé à cette performance");
            }
        }
        return toResponse(p);
    }

    public PerformanceResponse create(PerformanceRequest request) {
        Performance p = Performance.builder()
                .commercial(commercialRepository.findById(request.commercialId())
                        .orElseThrow(() -> new ResourceNotFoundException("Commercial introuvable : " + request.commercialId())))
                .periode(request.periode())
                .chiffreAffaires(request.chiffreAffaires() != null ? request.chiffreAffaires() : BigDecimal.ZERO)
                .nombreVisites(request.nombreVisites() != null ? request.nombreVisites() : 0)
                .tauxConversion(request.tauxConversion() != null ? request.tauxConversion() : 0.0)
                .build();
        return toResponse(performanceRepository.save(p));
    }

    public PerformanceResponse update(Long id, PerformanceRequest request) {
        Performance p = get(id);
        if (request.chiffreAffaires() != null) p.setChiffreAffaires(request.chiffreAffaires());
        if (request.nombreVisites() != null) p.setNombreVisites(request.nombreVisites());
        if (request.tauxConversion() != null) p.setTauxConversion(request.tauxConversion());
        return toResponse(performanceRepository.save(p));
    }

    public void delete(Long id) {
        performanceRepository.delete(get(id));
    }

    private Performance get(Long id) {
        return performanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Performance introuvable : " + id));
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
                .orElseThrow(() -> new ResourceNotFoundException("Profil commercial introuvable : " + username))
                .getId();
    }

    private PerformanceResponse toResponse(Performance p) {
        return new PerformanceResponse(
                p.getId(),
                p.getCommercial().getId(),
                p.getCommercial().getNom() + " " + p.getCommercial().getPrenom(),
                p.getPeriode(),
                p.getChiffreAffaires(),
                p.getNombreVisites(),
                p.getTauxConversion()
        );
    }
}
