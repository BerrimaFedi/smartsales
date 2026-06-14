package com.smartit.smartsales.dto.request;

import com.smartit.smartsales.domain.enums.StatutVisite;
import java.math.BigDecimal;

public record VisitePatchRequest(
        StatutVisite statut,
        String compteRendu,
        BigDecimal montant
) {}
