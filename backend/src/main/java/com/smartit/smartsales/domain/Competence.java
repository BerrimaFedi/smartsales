package com.smartit.smartsales.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "competences")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Competence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nom;
}
