package com.smartit.smartsales.dto.ai;

import java.util.List;

/** Corps de la réponse reçue depuis le service Python /analyze-performances. */
public record AnalyseResponseDto(
        List<AnalyseResultatDto> analyses
) {}
