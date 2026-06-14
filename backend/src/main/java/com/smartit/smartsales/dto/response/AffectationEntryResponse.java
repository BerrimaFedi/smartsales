package com.smartit.smartsales.dto.response;

import com.smartit.smartsales.domain.enums.TypeVisite;

/**
 * Détail d'une ligne dans le rapport d'affectation/réaffectation.
 * commercialId / commercialNom null => visite non affectable.
 */
public record AffectationEntryResponse(
        Long visiteId,
        String clientNom,
        TypeVisite type,
        Long commercialId,
        String commercialNom,
        String raison
) {}
