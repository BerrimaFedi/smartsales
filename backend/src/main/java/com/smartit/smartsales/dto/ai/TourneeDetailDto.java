package com.smartit.smartsales.dto.ai;

public record TourneeDetailDto(
        int ordre,
        int visiteId,
        String clientNom,
        double distanceDepuisPrecedentKm
) {}
