package com.smartit.smartsales.dto.request;

import com.smartit.smartsales.domain.enums.StatutVisite;
import com.smartit.smartsales.domain.enums.TypeVisite;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VisiteRequest(
        Long commercialId,
        @NotNull Long clientId,
        @NotNull LocalDateTime dateVisite,
        @NotNull TypeVisite type,
        StatutVisite statut,
        String compteRendu,
        Integer ordreTournee,
        BigDecimal montant
) {}
