package com.smartit.smartsales.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ZoneRequest(
        @NotBlank String nom,
        String description
) {}
