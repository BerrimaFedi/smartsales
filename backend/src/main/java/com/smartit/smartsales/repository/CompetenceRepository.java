package com.smartit.smartsales.repository;

import com.smartit.smartsales.domain.Competence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompetenceRepository extends JpaRepository<Competence, Long> {

    @Modifying
    @Query(value = "DELETE FROM commercial_competences WHERE competence_id = :id", nativeQuery = true)
    void removeFromAllCommercials(@Param("id") Long id);
}
