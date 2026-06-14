package com.smartit.smartsales.dto.request;

/// Corps des requêtes check-in / check-out : coordonnées GPS optionnelles.
public record CheckRequest(
        Double latitude,
        Double longitude
) {}
