package com.smartit.smartsales.dto.ai;

import java.math.BigDecimal;

/** Une période de performance utilisée comme entrée pour l'analyse IA. */
public record AnalyseHistoriqueItemDto(
        String periode,
        BigDecimal chiffreAffaires,
        int nombreVisites,
        int nombreVisitesTerminees,
        double tauxConversion
) {}
