package com.smartit.smartsales.dto.ai;

import java.util.List;

/** Résultat d'analyse IA pour un commercial — sert aussi de réponse vers le frontend. */
public record AnalyseResultatDto(
        Long commercialId,
        String commercialNom,
        String tendanceCA,
        Double variationCAPct,
        double tauxConversionMoyen,
        boolean anomalieDetectee,
        String anomalieDescription,
        List<String> recommandations
) {}
