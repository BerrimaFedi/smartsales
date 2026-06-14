package com.smartit.smartsales.service;

import com.smartit.smartsales.domain.Zone;
import com.smartit.smartsales.dto.request.ZoneRequest;
import com.smartit.smartsales.dto.response.ZoneResponse;
import com.smartit.smartsales.exception.ResourceNotFoundException;
import com.smartit.smartsales.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ZoneService {

    private final ZoneRepository zoneRepository;

    public List<ZoneResponse> findAll() {
        return zoneRepository.findAll().stream().map(this::toResponse).toList();
    }

    public ZoneResponse findById(Long id) {
        return toResponse(get(id));
    }

    public ZoneResponse create(ZoneRequest request) {
        Zone zone = Zone.builder().nom(request.nom()).description(request.description()).build();
        return toResponse(zoneRepository.save(zone));
    }

    public ZoneResponse update(Long id, ZoneRequest request) {
        Zone zone = get(id);
        zone.setNom(request.nom());
        zone.setDescription(request.description());
        return toResponse(zoneRepository.save(zone));
    }

    public void delete(Long id) {
        zoneRepository.delete(get(id));
    }

    Zone get(Long id) {
        return zoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zone introuvable : " + id));
    }

    public ZoneResponse toResponse(Zone z) {
        return new ZoneResponse(z.getId(), z.getNom(), z.getDescription());
    }
}
