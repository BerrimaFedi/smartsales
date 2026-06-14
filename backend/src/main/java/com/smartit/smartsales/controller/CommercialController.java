package com.smartit.smartsales.controller;

import com.smartit.smartsales.dto.request.CommercialRequest;
import com.smartit.smartsales.dto.response.CommercialResponse;
import com.smartit.smartsales.service.CommercialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/commerciaux")
@RequiredArgsConstructor
public class CommercialController {

    private final CommercialService commercialService;

    @GetMapping
    public List<CommercialResponse> findAll() { return commercialService.findAll(); }

    @GetMapping("/{id}")
    public CommercialResponse findById(@PathVariable Long id) { return commercialService.findById(id); }

    @GetMapping("/me")
    public CommercialResponse me(java.security.Principal principal) {
        return commercialService.findByUsername(principal.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommercialResponse create(@Valid @RequestBody CommercialRequest request) { return commercialService.create(request); }

    @PutMapping("/{id}")
    public CommercialResponse update(@PathVariable Long id, @Valid @RequestBody CommercialRequest request) { return commercialService.update(id, request); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { commercialService.delete(id); }
}
