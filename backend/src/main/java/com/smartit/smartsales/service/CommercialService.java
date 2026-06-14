package com.smartit.smartsales.service;

import com.smartit.smartsales.domain.Commercial;
import com.smartit.smartsales.domain.Competence;
import com.smartit.smartsales.domain.User;
import com.smartit.smartsales.domain.enums.Role;
import com.smartit.smartsales.dto.request.CommercialRequest;
import com.smartit.smartsales.dto.response.CommercialResponse;
import com.smartit.smartsales.dto.response.CompetenceResponse;
import com.smartit.smartsales.exception.ResourceNotFoundException;
import com.smartit.smartsales.repository.CommercialRepository;
import com.smartit.smartsales.repository.CompetenceRepository;
import com.smartit.smartsales.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommercialService {

    private final CommercialRepository commercialRepository;
    private final UserRepository userRepository;
    private final CompetenceRepository competenceRepository;
    private final ZoneService zoneService;
    private final PasswordEncoder passwordEncoder;

    public List<CommercialResponse> findAll() {
        return commercialRepository.findAll().stream().map(this::toResponse).toList();
    }

    public CommercialResponse findById(Long id) {
        return toResponse(get(id));
    }

    public CommercialResponse findByUsername(String username) {
        Commercial c = commercialRepository.findByUserUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Commercial introuvable pour : " + username));
        return toResponse(c);
    }

    @Transactional
    public CommercialResponse create(CommercialRequest request) {
        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("Le mot de passe est obligatoire");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username déjà utilisé : " + request.username());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email déjà utilisé : " + request.email());
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.COMMERCIAL)
                .enabled(true)
                .build();
        userRepository.save(user);

        Commercial commercial = Commercial.builder()
                .nom(request.nom())
                .prenom(request.prenom())
                .telephone(request.telephone())
                .zone(request.zoneId() != null ? zoneService.get(request.zoneId()) : null)
                .competences(resolveCompetences(request.competenceIds()))
                .user(user)
                .build();
        return toResponse(commercialRepository.save(commercial));
    }

    @Transactional
    public CommercialResponse update(Long id, CommercialRequest request) {
        Commercial commercial = get(id);
        User user = commercial.getUser();

        if (user != null) {
            // Unicité username — vérifié seulement si la valeur change
            if (!user.getUsername().equals(request.username())
                    && userRepository.existsByUsername(request.username())) {
                throw new IllegalArgumentException("Username déjà utilisé : " + request.username());
            }
            // Unicité email — vérifié seulement si la valeur change
            if (!user.getEmail().equals(request.email())
                    && userRepository.existsByEmail(request.email())) {
                throw new IllegalArgumentException("Email déjà utilisé : " + request.email());
            }
            user.setUsername(request.username());
            user.setEmail(request.email());
            // Password inchangé si le champ est vide/absent
            if (request.password() != null && !request.password().isBlank()) {
                user.setPassword(passwordEncoder.encode(request.password()));
            }
            userRepository.save(user);
        }

        commercial.setNom(request.nom());
        commercial.setPrenom(request.prenom());
        commercial.setTelephone(request.telephone());
        commercial.setZone(request.zoneId() != null ? zoneService.get(request.zoneId()) : null);
        commercial.setCompetences(resolveCompetences(request.competenceIds()));
        return toResponse(commercialRepository.save(commercial));
    }

    @Transactional
    public void delete(Long id) {
        Commercial commercial = get(id);
        User user = commercial.getUser();
        // Supprime d'abord le commercial (retire la FK user_id), puis le User
        commercialRepository.delete(commercial);
        commercialRepository.flush();
        if (user != null) {
            userRepository.delete(user);
        }
    }

    Commercial get(Long id) {
        return commercialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commercial introuvable : " + id));
    }

    private Set<Competence> resolveCompetences(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) return new HashSet<>();
        return new HashSet<>(competenceRepository.findAllById(ids));
    }

    public CommercialResponse toResponse(Commercial c) {
        Set<CompetenceResponse> comps = c.getCompetences().stream()
                .map(comp -> new CompetenceResponse(comp.getId(), comp.getNom()))
                .collect(Collectors.toSet());
        return new CommercialResponse(
                c.getId(), c.getNom(), c.getPrenom(), c.getTelephone(),
                c.getUser() != null ? c.getUser().getUsername() : null,
                c.getUser() != null ? c.getUser().getEmail() : null,
                c.getZone() != null ? zoneService.toResponse(c.getZone()) : null,
                comps
        );
    }
}
