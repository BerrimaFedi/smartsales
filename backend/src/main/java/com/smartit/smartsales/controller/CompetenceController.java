package com.smartit.smartsales.controller;

import com.smartit.smartsales.dto.request.CompetenceRequest;
import com.smartit.smartsales.dto.response.CompetenceResponse;
import com.smartit.smartsales.service.CompetenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/competences")
@RequiredArgsConstructor
public class CompetenceController {

    private final CompetenceService competenceService;

    @GetMapping
    public List<CompetenceResponse> findAll() { return competenceService.findAll(); }

    @GetMapping("/{id}")
    public CompetenceResponse findById(@PathVariable Long id) { return competenceService.findById(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompetenceResponse create(@Valid @RequestBody CompetenceRequest request) { return competenceService.create(request); }

    @PutMapping("/{id}")
    public CompetenceResponse update(@PathVariable Long id, @Valid @RequestBody CompetenceRequest request) { return competenceService.update(id, request); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { competenceService.delete(id); }
}
