package com.smartit.smartsales.dto.ai;

import java.util.List;

public record OptimizeResponseDto(
        List<Integer> ordre,
        double distanceTotaleKm,
        List<TourneeDetailDto> details
) {}
