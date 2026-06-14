package com.smartit.smartsales.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public record CommercialRequest(
        @NotBlank String nom,
        @NotBlank String prenom,
        String telephone,
        Long zoneId,
        Set<Long> competenceIds,
        @NotBlank String username,
        @NotBlank @Email String email,
        String password   // obligatoire à la création, optionnel à l'édition (validé dans le service)
) {}
