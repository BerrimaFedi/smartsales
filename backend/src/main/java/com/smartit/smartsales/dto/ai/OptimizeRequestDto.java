package com.smartit.smartsales.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record OptimizeRequestDto(
        @JsonProperty("departLatitude")  Double departLatitude,
        @JsonProperty("departLongitude") Double departLongitude,
        @JsonProperty("visites")         List<VisitePointDto> visites
) {}
