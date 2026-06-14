package com.smartit.smartsales.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VisitePointDto(
        @JsonProperty("id")        int    id,
        @JsonProperty("clientNom") String clientNom,
        @JsonProperty("latitude")  double latitude,
        @JsonProperty("longitude") double longitude,
        @JsonProperty("type")      String type,
        @JsonProperty("priorite")  int    priorite
) {}
