package com.smartit.smartsales.repository;

import com.smartit.smartsales.domain.Performance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PerformanceRepository extends JpaRepository<Performance, Long> {
    List<Performance> findByCommercialId(Long commercialId);
    Optional<Performance> findByCommercialIdAndPeriode(Long commercialId, String periode);
}
