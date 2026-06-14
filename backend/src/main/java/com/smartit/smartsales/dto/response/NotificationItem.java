package com.smartit.smartsales.dto.response;

import java.time.LocalDateTime;

/**
 * Représente une notification individuelle calculée à la volée.
 *
 * @param type      "RAPPEL", "RETARD" ou "SUGGESTION"
 * @param titre     Titre court affiché dans le panneau
 * @param message   Description détaillée
 * @param date      Date de référence (visite concernée ou instant de calcul)
 * @param severite  "info" (rappels, suggestions) ou "warning" (retards)
 */
public record NotificationItem(
        String type,
        String titre,
        String message,
        LocalDateTime date,
        String severite
) {}
