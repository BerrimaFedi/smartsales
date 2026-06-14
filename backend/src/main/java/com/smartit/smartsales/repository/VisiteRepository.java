package com.smartit.smartsales.repository;

import com.smartit.smartsales.domain.Visite;
import com.smartit.smartsales.domain.enums.StatutVisite;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface VisiteRepository extends JpaRepository<Visite, Long> {
    List<Visite> findByCommercialId(Long commercialId);
    List<Visite> findByCommercialIdAndDateVisiteBetween(Long commercialId, LocalDateTime debut, LocalDateTime fin);
    List<Visite> findByDateVisiteBetween(LocalDateTime debut, LocalDateTime fin);
    List<Visite> findByCommercialIdAndStatutAndDateVisiteBetween(
            Long commercialId, StatutVisite statut, LocalDateTime debut, LocalDateTime fin);

    // Visites non assignées (commercial null) filtrées par statut + période
    List<Visite> findByCommercialIsNullAndStatutAndDateVisiteBetween(
            StatutVisite statut, LocalDateTime debut, LocalDateTime fin);

    // Visites par IDs explicites
    List<Visite> findByIdIn(List<Long> ids);

    // Visites d'un commercial avec statut parmi une liste (pour réaffectation)
    List<Visite> findByCommercialIdAndStatutInAndDateVisiteBetween(
            Long commercialId, List<StatutVisite> statuts, LocalDateTime debut, LocalDateTime fin);

    // Compteurs pour le calcul de charge
    long countByCommercialIdAndDateVisiteBetween(Long commercialId, LocalDateTime debut, LocalDateTime fin);
    long countByCommercialId(Long commercialId);
}
