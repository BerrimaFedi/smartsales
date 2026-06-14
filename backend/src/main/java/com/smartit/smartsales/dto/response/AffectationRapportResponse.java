package com.smartit.smartsales.dto.response;

import java.util.List;

/** Rapport complet retourné par les endpoints d'affectation et de réaffectation. */
public record AffectationRapportResponse(
        List<AffectationEntryResponse> affectees,
        List<AffectationEntryResponse> nonAffectables
) {}
