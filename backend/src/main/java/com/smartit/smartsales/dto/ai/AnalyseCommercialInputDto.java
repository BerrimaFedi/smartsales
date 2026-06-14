package com.smartit.smartsales.dto.ai;

import java.util.List;

/** Un commercial avec tout son historique de performances, envoyé au service Python. */
public record AnalyseCommercialInputDto(
        Long commercialId,
        String commercialNom,
        List<AnalyseHistoriqueItemDto> historique
) {}
