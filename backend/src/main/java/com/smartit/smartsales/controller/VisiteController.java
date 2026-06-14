package com.smartit.smartsales.controller;

import com.smartit.smartsales.dto.request.CheckRequest;
import com.smartit.smartsales.dto.request.VisitePatchRequest;
import com.smartit.smartsales.dto.request.VisiteRequest;
import com.smartit.smartsales.dto.response.VisiteResponse;
import com.smartit.smartsales.service.VisiteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/visites")
@RequiredArgsConstructor
public class VisiteController {

    private final VisiteService visiteService;

    @GetMapping
    public List<VisiteResponse> findAll() { return visiteService.findAll(); }

    @GetMapping("/{id}")
    public VisiteResponse findById(@PathVariable Long id) { return visiteService.findById(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VisiteResponse create(@Valid @RequestBody VisiteRequest request) { return visiteService.create(request); }

    @PutMapping("/{id}")
    public VisiteResponse update(@PathVariable Long id, @Valid @RequestBody VisiteRequest request) { return visiteService.update(id, request); }

    @PatchMapping("/{id}")
    public VisiteResponse patch(@PathVariable Long id, @RequestBody VisitePatchRequest request) {
        return visiteService.patch(id, request);
    }

    @PostMapping("/{id}/checkin")
    public VisiteResponse checkIn(
            @PathVariable Long id,
            @RequestBody(required = false) CheckRequest request) {
        return visiteService.checkIn(id, request);
    }

    @PostMapping("/{id}/checkout")
    public VisiteResponse checkOut(
            @PathVariable Long id,
            @RequestBody(required = false) CheckRequest request) {
        return visiteService.checkOut(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { visiteService.delete(id); }
}
