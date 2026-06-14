package com.smartit.smartsales.service;

import com.smartit.smartsales.domain.Competence;
import com.smartit.smartsales.dto.request.CompetenceRequest;
import com.smartit.smartsales.dto.response.CompetenceResponse;
import com.smartit.smartsales.exception.ResourceNotFoundException;
import com.smartit.smartsales.repository.CompetenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompetenceService {

    private final CompetenceRepository competenceRepository;

    public List<CompetenceResponse> findAll() {
        return competenceRepository.findAll().stream().map(this::toResponse).toList();
    }

    public CompetenceResponse findById(Long id) {
        return toResponse(get(id));
    }

    public CompetenceResponse create(CompetenceRequest request) {
        Competence c = Competence.builder().nom(request.nom()).build();
        return toResponse(competenceRepository.save(c));
    }

    public CompetenceResponse update(Long id, CompetenceRequest request) {
        Competence c = get(id);
        c.setNom(request.nom());
        return toResponse(competenceRepository.save(c));
    }

    @Transactional
    public void delete(Long id) {
        get(id); // lance 404 si introuvable
        competenceRepository.removeFromAllCommercials(id);
        competenceRepository.deleteById(id);
    }

    Competence get(Long id) {
        return competenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compétence introuvable : " + id));
    }

    public CompetenceResponse toResponse(Competence c) {
        return new CompetenceResponse(c.getId(), c.getNom());
    }
}
