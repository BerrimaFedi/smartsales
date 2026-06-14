package com.smartit.smartsales.service;

import com.smartit.smartsales.domain.Client;
import com.smartit.smartsales.dto.request.ClientRequest;
import com.smartit.smartsales.dto.response.ClientResponse;
import com.smartit.smartsales.exception.ResourceNotFoundException;
import com.smartit.smartsales.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final ZoneService zoneService;

    public List<ClientResponse> findAll() {
        return clientRepository.findAll().stream().map(this::toResponse).toList();
    }

    public ClientResponse findById(Long id) {
        return toResponse(get(id));
    }

    public ClientResponse create(ClientRequest request) {
        Client client = Client.builder()
                .nom(request.nom())
                .adresse(request.adresse())
                .telephone(request.telephone())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .zone(request.zoneId() != null ? zoneService.get(request.zoneId()) : null)
                .build();
        return toResponse(clientRepository.save(client));
    }

    public ClientResponse update(Long id, ClientRequest request) {
        Client client = get(id);
        client.setNom(request.nom());
        client.setAdresse(request.adresse());
        client.setTelephone(request.telephone());
        client.setLatitude(request.latitude());
        client.setLongitude(request.longitude());
        client.setZone(request.zoneId() != null ? zoneService.get(request.zoneId()) : null);
        return toResponse(clientRepository.save(client));
    }

    public void delete(Long id) {
        clientRepository.delete(get(id));
    }

    Client get(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable : " + id));
    }

    public ClientResponse toResponse(Client c) {
        return new ClientResponse(
                c.getId(), c.getNom(), c.getAdresse(), c.getTelephone(),
                c.getLatitude(), c.getLongitude(),
                c.getZone() != null ? zoneService.toResponse(c.getZone()) : null
        );
    }
}
