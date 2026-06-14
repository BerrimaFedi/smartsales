package com.smartit.smartsales.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record PerformanceRequest(
        @NotNull Long commercialId,
        @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}") String periode,
        BigDecimal chiffreAffaires,
        Integer nombreVisites,
        Double tauxConversion
) {}
