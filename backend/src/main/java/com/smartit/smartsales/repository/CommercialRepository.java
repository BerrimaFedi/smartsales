package com.smartit.smartsales.repository;

import com.smartit.smartsales.domain.Commercial;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CommercialRepository extends JpaRepository<Commercial, Long> {
    Optional<Commercial> findByUserId(Long userId);
    Optional<Commercial> findByUserUsername(String username);
}
