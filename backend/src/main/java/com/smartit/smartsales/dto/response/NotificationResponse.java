package com.smartit.smartsales.dto.response;

import java.util.List;

/**
 * Réponse de l'endpoint GET /api/notifications.
 *
 * @param count         Nombre total de notifications (utilisé pour le badge)
 * @param notifications Liste triée (retards en premier, puis par date)
 */
public record NotificationResponse(
        int count,
        List<NotificationItem> notifications
) {}
