package com.smartit.smartsales.dto.response;

import java.math.BigDecimal;

public record PerformanceCalculeeResponse(
        Long commercialId,
        String commercialNom,
        String periode,
        BigDecimal chiffreAffaires,
        int nombreVisites,
        int nombreVisitesTerminees,
        double tauxConversion,
        boolean isManuel,
        Long performanceManuelleId
) {}
