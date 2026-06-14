package com.smartit.smartsales.service;

import com.smartit.smartsales.domain.Commercial;
import com.smartit.smartsales.domain.Performance;
import com.smartit.smartsales.domain.Visite;
import com.smartit.smartsales.domain.enums.Role;
import com.smartit.smartsales.domain.enums.StatutVisite;
import com.smartit.smartsales.dto.response.PerformanceCalculeeResponse;
import com.smartit.smartsales.exception.ResourceNotFoundException;
import com.smartit.smartsales.repository.CommercialRepository;
import com.smartit.smartsales.repository.PerformanceRepository;
import com.smartit.smartsales.repository.UserRepository;
import com.smartit.smartsales.repository.VisiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

@Service
@RequiredArgsConstructor
public class PerformanceCalculeeService {

    private final VisiteRepository visiteRepository;
    private final CommercialRepository commercialRepository;
    private final PerformanceRepository performanceRepository;
    private final UserRepository userRepository;

    public List<PerformanceCalculeeResponse> calculerTous(String periode) {
        String p = resoudreOuMoisCourant(periode);
        return commercialRepository.findAll().stream()
                .map(c -> calculer(c, p))
                .toList();
    }

    public PerformanceCalculeeResponse calculerPourMoi(String periode) {
        String username = currentUsername();
        if (!isCommercial(username)) throw new AccessDeniedException("Réservé aux commerciaux");
        Commercial commercial = commercialRepository.findByUserUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Profil commercial introuvable : " + username));
        return calculer(commercial, resoudreOuMoisCourant(periode));
    }

    /**
     * Retourne l'historique complet des performances calculées pour un commercial
     * (toutes les périodes pour lesquelles il a des visites ou une saisie manuelle).
     */
    public List<PerformanceCalculeeResponse> calculerHistorique(Commercial commercial) {
        Set<String> periodes = new TreeSet<>();

        // Périodes issues des saisies manuelles
        performanceRepository.findByCommercialId(commercial.getId())
                .forEach(p -> periodes.add(p.getPeriode()));

        // Périodes issues des visites (extraction AAAA-MM depuis LocalDateTime)
        visiteRepository.findByCommercialId(commercial.getId())
                .forEach(v -> periodes.add(extractPeriode(v.getDateVisite())));

        return periodes.stream()
                .sorted()
                .map(p -> calculer(commercial, p))
                .toList();
    }

    /** Génère le rapport CSV des performances calculées pour la période donnée. */
    public byte[] genererCsv(String periode) {
        String p = resoudreOuMoisCourant(periode);
        List<PerformanceCalculeeResponse> performances = calculerTous(p);

        StringBuilder sb = new StringBuilder("﻿"); // BOM UTF-8 pour Excel
        sb.append("Commercial;Période;CA (DT);Nb visites;Nb terminées;Taux conversion\n");

        for (PerformanceCalculeeResponse perf : performances) {
            sb.append(escapeCsv(perf.commercialNom())).append(";")
              .append(perf.periode()).append(";")
              .append(perf.chiffreAffaires().toPlainString()).append(";")
              .append(perf.nombreVisites()).append(";")
              .append(perf.nombreVisitesTerminees()).append(";")
              .append(String.format("%.1f%%", perf.tauxConversion() * 100)).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String extractPeriode(LocalDateTime dt) {
        return dt.format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(";") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private PerformanceCalculeeResponse calculer(Commercial commercial, String periode) {
        LocalDateTime[] bornes = periodeBornes(periode);
        List<Visite> visites = visiteRepository.findByCommercialIdAndDateVisiteBetween(
                commercial.getId(), bornes[0], bornes[1]);

        int nombreVisites = visites.size();
        int nombreTerminees = (int) visites.stream()
                .filter(v -> v.getStatut() == StatutVisite.TERMINEE).count();
        double taux = nombreVisites > 0 ? (double) nombreTerminees / nombreVisites : 0.0;
        BigDecimal ca = visites.stream()
                .filter(v -> v.getStatut() == StatutVisite.TERMINEE)
                .map(v -> v.getMontant() != null ? v.getMontant() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Optional<Performance> manuel = performanceRepository.findByCommercialIdAndPeriode(
                commercial.getId(), periode);

        if (manuel.isPresent()) {
            Performance p = manuel.get();
            return new PerformanceCalculeeResponse(
                    commercial.getId(),
                    commercial.getNom() + " " + commercial.getPrenom(),
                    periode,
                    p.getChiffreAffaires(),
                    p.getNombreVisites(),
                    nombreTerminees,
                    p.getTauxConversion(),
                    true,
                    p.getId()
            );
        }

        return new PerformanceCalculeeResponse(
                commercial.getId(),
                commercial.getNom() + " " + commercial.getPrenom(),
                periode,
                ca,
                nombreVisites,
                nombreTerminees,
                taux,
                false,
                null
        );
    }

    private String resoudreOuMoisCourant(String periode) {
        if (periode != null && !periode.isBlank()) {
            try {
                YearMonth.parse(periode);
                return periode;
            } catch (DateTimeParseException ignored) {}
        }
        YearMonth now = YearMonth.now();
        return now.toString();
    }

    private LocalDateTime[] periodeBornes(String periode) {
        YearMonth ym = YearMonth.parse(periode);
        LocalDateTime debut = ym.atDay(1).atStartOfDay();
        LocalDateTime fin = ym.atEndOfMonth().atTime(23, 59, 59);
        return new LocalDateTime[]{debut, fin};
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
