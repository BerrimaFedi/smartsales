package com.smartit.smartsales.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Corps de la requête POST /api/planning/reaffecter.
 * commercialId : le commercial devenu indisponible
 * dateDebut    : début de la plage (défaut = aujourd'hui)
 * dateFin      : fin de la plage (défaut = dateDebut + 30 jours)
 */
public record ReaffectationRequest(
        @NotNull Long commercialId,
        LocalDate dateDebut,
        LocalDate dateFin
) {}
