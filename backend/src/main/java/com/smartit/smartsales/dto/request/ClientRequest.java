package com.smartit.smartsales.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ClientRequest(
        @NotBlank String nom,
        String adresse,
        String telephone,
        Double latitude,
        Double longitude,
        Long zoneId
) {}
