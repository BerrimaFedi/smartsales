package com.smartit.smartsales.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Réponse de GET /api/dashboard/stats — tous les agrégats BI en un seul appel.
 * Calculé à la volée, réservé MANAGER/ADMIN.
 */
public record DashboardStatsResponse(
        List<CaParCommercialEntry> caParCommercial,
        VisitesParStatut           visitesParStatut,
        VisitesParType             visitesParType,
        List<CaParMoisEntry>       caParMois
) {
    /** CA du mois courant pour un commercial (visites TERMINEE). */
    public record CaParCommercialEntry(String commercial, BigDecimal ca) {}

    /** Totaux de visites sur les 6 derniers mois (une entrée par mois, format "AAAA-MM"). */
    public record CaParMoisEntry(String periode, BigDecimal ca) {}

    /** Répartition globale des visites par statut. */
    public record VisitesParStatut(long planifiee, long enCours, long terminee, long annulee) {}

    /** Répartition globale des visites par type. */
    public record VisitesParType(long prospection, long relance, long negociation) {}
}
