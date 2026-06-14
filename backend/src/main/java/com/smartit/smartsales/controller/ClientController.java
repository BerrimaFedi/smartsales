package com.smartit.smartsales.controller;

import com.smartit.smartsales.dto.request.ClientRequest;
import com.smartit.smartsales.dto.response.ClientResponse;
import com.smartit.smartsales.service.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @GetMapping
    public List<ClientResponse> findAll() { return clientService.findAll(); }

    @GetMapping("/{id}")
    public ClientResponse findById(@PathVariable Long id) { return clientService.findById(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientResponse create(@Valid @RequestBody ClientRequest request) { return clientService.create(request); }

    @PutMapping("/{id}")
    public ClientResponse update(@PathVariable Long id, @Valid @RequestBody ClientRequest request) { return clientService.update(id, request); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { clientService.delete(id); }
}
