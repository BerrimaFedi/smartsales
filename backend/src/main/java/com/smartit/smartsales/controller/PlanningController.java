package com.smartit.smartsales.controller;

import com.smartit.smartsales.dto.request.AffectationAutoRequest;
import com.smartit.smartsales.dto.request.ReaffectationRequest;
import com.smartit.smartsales.dto.response.AffectationRapportResponse;
import com.smartit.smartsales.dto.response.PlanningResponse;
import com.smartit.smartsales.service.AffectationService;
import com.smartit.smartsales.service.PlanningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/planning")
@RequiredArgsConstructor
public class PlanningController {

    private final PlanningService planningService;
    private final AffectationService affectationService;

    /** Optimisation de tournée par l'agent IA (accessible à tous les rôles authentifiés). */
    @PostMapping("/optimiser")
    public PlanningResponse optimiser(
            @RequestParam Long commercialId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return planningService.optimiserTournee(commercialId, date);
    }

    /**
     * Affectation automatique des visites non assignées.
     * Réservé MANAGER/ADMIN.
     */
    @PostMapping("/affecter-auto")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public AffectationRapportResponse affecterAuto(@RequestBody AffectationAutoRequest request) {
        return affectationService.affecterAuto(request);
    }

    /**
     * Réaffectation des visites d'un commercial devenu indisponible.
     * Réservé MANAGER/ADMIN.
     */
    @PostMapping("/reaffecter")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public AffectationRapportResponse reaffecter(@Valid @RequestBody ReaffectationRequest request) {
        return affectationService.reaffecter(request);
    }
}
