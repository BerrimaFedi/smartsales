package com.smartit.smartsales.controller;

import com.smartit.smartsales.dto.ai.AnalyseResultatDto;
import com.smartit.smartsales.dto.request.PerformanceRequest;
import com.smartit.smartsales.dto.response.PerformanceCalculeeResponse;
import com.smartit.smartsales.dto.response.PerformanceResponse;
import com.smartit.smartsales.service.PerformanceAnalyseService;
import com.smartit.smartsales.service.PerformanceCalculeeService;
import com.smartit.smartsales.service.PerformanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/performances")
@RequiredArgsConstructor
public class PerformanceController {

    private final PerformanceService performanceService;
    private final PerformanceCalculeeService performanceCalculeeService;
    private final PerformanceAnalyseService performanceAnalyseService;

    @GetMapping
    public List<PerformanceResponse> findAll() { return performanceService.findAll(); }

    @GetMapping("/{id}")
    public PerformanceResponse findById(@PathVariable Long id) { return performanceService.findById(id); }

    @GetMapping("/calculees")
    public List<PerformanceCalculeeResponse> findAllCalculees(
            @RequestParam(required = false) String periode) {
        return performanceCalculeeService.calculerTous(periode);
    }

    @GetMapping("/calculees/me")
    public PerformanceCalculeeResponse findMaCalculee(
            @RequestParam(required = false) String periode) {
        return performanceCalculeeService.calculerPourMoi(periode);
    }

    @GetMapping("/rapport")
    public ResponseEntity<byte[]> rapport(@RequestParam(required = false) String periode) {
        byte[] csv = performanceCalculeeService.genererCsv(periode);
        String filename = "performances-" + (periode != null && !periode.isBlank() ? periode : "courant") + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(csv);
    }

    @GetMapping("/analyse")
    public AnalyseResultatDto getAnalyse(@RequestParam Long commercialId) {
        return performanceAnalyseService.analyserCommercial(commercialId);
    }

    @GetMapping("/analyse/me")
    public AnalyseResultatDto getAnalysePourMoi() {
        return performanceAnalyseService.analyserPourMoi();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PerformanceResponse create(@Valid @RequestBody PerformanceRequest request) { return performanceService.create(request); }

    @PutMapping("/{id}")
    public PerformanceResponse update(@PathVariable Long id, @Valid @RequestBody PerformanceRequest request) { return performanceService.update(id, request); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { performanceService.delete(id); }
}
