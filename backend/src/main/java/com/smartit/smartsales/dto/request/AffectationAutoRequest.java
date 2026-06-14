package com.smartit.smartsales.dto.request;

import java.time.LocalDate;
import java.util.List;

/**
 * Corps de la requête POST /api/planning/affecter-auto.
 * date     : jour ciblé (défaut = aujourd'hui si absent)
 * visiteIds: si fourni, n'affecter QUE ces visites (doivent être PLANIFIEE + non assignées)
 */
public record AffectationAutoRequest(
        LocalDate date,
        List<Long> visiteIds
) {}
