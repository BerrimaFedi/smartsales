package com.smartit.smartsales.controller;

import com.smartit.smartsales.dto.response.DashboardStatsResponse;
import com.smartit.smartsales.service.DashboardStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expose les agrégats BI pour le tableau de bord MANAGER/ADMIN.
 * La restriction de rôle est configurée dans SecurityConfig.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardStatsService dashboardStatsService;

    @GetMapping("/stats")
    public DashboardStatsResponse getStats() {
        return dashboardStatsService.calculer();
    }
}
