package com.smartit.smartsales.dto.response;

public record ClientResponse(
        Long id,
        String nom,
        String adresse,
        String telephone,
        Double latitude,
        Double longitude,
        ZoneResponse zone
) {}
