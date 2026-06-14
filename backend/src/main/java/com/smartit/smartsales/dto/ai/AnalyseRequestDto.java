package com.smartit.smartsales.dto.ai;

import java.util.List;

/** Corps de la requête POST /analyze-performances envoyée au service Python. */
public record AnalyseRequestDto(
        List<AnalyseCommercialInputDto> commerciaux
) {}
