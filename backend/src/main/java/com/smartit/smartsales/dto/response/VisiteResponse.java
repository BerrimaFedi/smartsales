package com.smartit.smartsales.dto.response;

import com.smartit.smartsales.domain.enums.StatutVisite;
import com.smartit.smartsales.domain.enums.TypeVisite;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VisiteResponse(
        Long id,
        Long commercialId,
        String commercialNom,
        Long clientId,
        String clientNom,
        ClientInfo client,
        LocalDateTime dateVisite,
        TypeVisite type,
        StatutVisite statut,
        String compteRendu,
        Integer ordreTournee,
        LocalDateTime checkIn,
        LocalDateTime checkOut,
        Double checkInLatitude,
        Double checkInLongitude,
        Double checkOutLatitude,
        Double checkOutLongitude,
        BigDecimal montant
) {
    public record ClientInfo(
            Long id,
            String nom,
            String adresse,
            Double latitude,
            Double longitude
    ) {}
}
