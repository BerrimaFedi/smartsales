package com.smartit.smartsales.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "zones")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Zone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    private String description;
}
