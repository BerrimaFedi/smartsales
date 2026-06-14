package com.smartit.smartsales.dto.response;

import java.math.BigDecimal;

public record PerformanceResponse(
        Long id,
        Long commercialId,
        String commercialNom,
        String periode,
        BigDecimal chiffreAffaires,
        int nombreVisites,
        double tauxConversion
) {}
