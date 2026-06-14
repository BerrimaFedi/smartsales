package com.smartit.smartsales.service;

import com.smartit.smartsales.domain.Visite;
import com.smartit.smartsales.domain.enums.StatutVisite;
import com.smartit.smartsales.domain.enums.TypeVisite;
import com.smartit.smartsales.dto.response.DashboardStatsResponse;
import com.smartit.smartsales.repository.VisiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calcule à la volée les agrégats BI pour le tableau de bord MANAGER/ADMIN.
 * Aucune persistance : chaque appel relit toutes les visites et recalcule.
 */
@Service
@RequiredArgsConstructor
public class DashboardStatsService {

    private final VisiteRepository visiteRepository;

    /** Point d'entrée : charge toutes les visites et retourne les 4 agrégats. */
    public DashboardStatsResponse calculer() {
        List<Visite> toutes = visiteRepository.findAll();
        return new DashboardStatsResponse(
                calculerCaParCommercial(toutes),
                calculerVisitesParStatut(toutes),
                calculerVisitesParType(toutes),
                calculerCaParMois(toutes)
        );
    }

    // ── Agrégat 1 : CA par commercial (mois courant, visites TERMINEE) ────────

    private List<DashboardStatsResponse.CaParCommercialEntry> calculerCaParCommercial(List<Visite> visites) {
        YearMonth mois  = YearMonth.now();
        LocalDateTime debut = mois.atDay(1).atStartOfDay();
        LocalDateTime fin   = mois.atEndOfMonth().atTime(23, 59, 59);

        Map<String, BigDecimal> parCommercial = new LinkedHashMap<>();
        visites.stream()
                .filter(v -> v.getStatut() == StatutVisite.TERMINEE
                          && v.getCommercial() != null
                          && !v.getDateVisite().isBefore(debut)
                          && !v.getDateVisite().isAfter(fin))
                .forEach(v -> {
                    String nom = v.getCommercial().getNom() + " " + v.getCommercial().getPrenom();
                    BigDecimal montant = v.getMontant() != null ? v.getMontant() : BigDecimal.ZERO;
                    parCommercial.merge(nom, montant, BigDecimal::add);
                });

        // Tri décroissant par CA
        return parCommercial.entrySet().stream()
                .map(e -> new DashboardStatsResponse.CaParCommercialEntry(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(DashboardStatsResponse.CaParCommercialEntry::ca).reversed())
                .toList();
    }

    // ── Agrégat 2 : répartition des visites par statut (toutes périodes) ─────

    private DashboardStatsResponse.VisitesParStatut calculerVisitesParStatut(List<Visite> visites) {
        return new DashboardStatsResponse.VisitesParStatut(
                visites.stream().filter(v -> v.getStatut() == StatutVisite.PLANIFIEE).count(),
                visites.stream().filter(v -> v.getStatut() == StatutVisite.EN_COURS).count(),
                visites.stream().filter(v -> v.getStatut() == StatutVisite.TERMINEE).count(),
                visites.stream().filter(v -> v.getStatut() == StatutVisite.ANNULEE).count()
        );
    }

    // ── Agrégat 3 : répartition des visites par type (toutes périodes) ───────

    private DashboardStatsResponse.VisitesParType calculerVisitesParType(List<Visite> visites) {
        return new DashboardStatsResponse.VisitesParType(
                visites.stream().filter(v -> v.getType() == TypeVisite.PROSPECTION).count(),
                visites.stream().filter(v -> v.getType() == TypeVisite.RELANCE).count(),
                visites.stream().filter(v -> v.getType() == TypeVisite.NEGOCIATION).count()
        );
    }

    // ── Agrégat 4 : CA de l'équipe sur les 6 derniers mois ───────────────────

    private List<DashboardStatsResponse.CaParMoisEntry> calculerCaParMois(List<Visite> visites) {
        YearMonth moisCourant = YearMonth.now();
        List<DashboardStatsResponse.CaParMoisEntry> resultat = new ArrayList<>();

        for (int i = 5; i >= 0; i--) {
            YearMonth mois  = moisCourant.minusMonths(i);
            LocalDateTime debut = mois.atDay(1).atStartOfDay();
            LocalDateTime fin   = mois.atEndOfMonth().atTime(23, 59, 59);

            BigDecimal ca = visites.stream()
                    .filter(v -> v.getStatut() == StatutVisite.TERMINEE
                              && !v.getDateVisite().isBefore(debut)
                              && !v.getDateVisite().isAfter(fin))
                    .map(v -> v.getMontant() != null ? v.getMontant() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            resultat.add(new DashboardStatsResponse.CaParMoisEntry(mois.toString(), ca));
        }
        return resultat;
    }
}
