package com.smartit.smartsales.controller;

import com.smartit.smartsales.dto.response.NotificationResponse;
import com.smartit.smartsales.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expose le calcul à la volée des notifications intelligentes.
 * Accessible à tous les rôles connectés (COMMERCIAL, MANAGER, ADMIN).
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public NotificationResponse getNotifications() {
        return notificationService.calculer();
    }
}
