package com.smartit.smartsales.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CompetenceRequest(
        @NotBlank String nom
) {}
