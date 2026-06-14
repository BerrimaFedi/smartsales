package com.smartit.smartsales.controller;

import com.smartit.smartsales.dto.request.ZoneRequest;
import com.smartit.smartsales.dto.response.ZoneResponse;
import com.smartit.smartsales.service.ZoneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/zones")
@RequiredArgsConstructor
public class ZoneController {

    private final ZoneService zoneService;

    @GetMapping
    public List<ZoneResponse> findAll() { return zoneService.findAll(); }

    @GetMapping("/{id}")
    public ZoneResponse findById(@PathVariable Long id) { return zoneService.findById(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ZoneResponse create(@Valid @RequestBody ZoneRequest request) { return zoneService.create(request); }

    @PutMapping("/{id}")
    public ZoneResponse update(@PathVariable Long id, @Valid @RequestBody ZoneRequest request) { return zoneService.update(id, request); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { zoneService.delete(id); }
}
