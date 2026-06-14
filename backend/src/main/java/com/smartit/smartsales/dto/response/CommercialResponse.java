package com.smartit.smartsales.dto.response;

import java.util.Set;

public record CommercialResponse(
        Long id,
        String nom,
        String prenom,
        String telephone,
        String username,
        String email,
        ZoneResponse zone,
        Set<CompetenceResponse> competences
) {}
