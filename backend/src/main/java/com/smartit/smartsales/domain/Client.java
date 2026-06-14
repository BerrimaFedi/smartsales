package com.smartit.smartsales.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "clients")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    private String adresse;
    private String telephone;

    // Geolocalisation -> indispensable pour l'optimisation des tournees par l'IA
    private Double latitude;
    private Double longitude;

    @ManyToOne
    @JoinColumn(name = "zone_id")
    private Zone zone;
}
