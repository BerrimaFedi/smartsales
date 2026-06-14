package com.smartit.smartsales.dto.response;

import java.util.List;

public record PlanningResponse(
        List<VisiteResponse> visites,
        double distanceTotaleKm
) {}
