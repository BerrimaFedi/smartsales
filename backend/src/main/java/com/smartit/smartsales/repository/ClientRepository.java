package com.smartit.smartsales.repository;

import com.smartit.smartsales.domain.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClientRepository extends JpaRepository<Client, Long> {
    List<Client> findByZoneId(Long zoneId);
}
