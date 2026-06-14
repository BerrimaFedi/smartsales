package com.smartit.smartsales.domain;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "commerciaux")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Commercial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    private String telephone;

    @ManyToOne
    @JoinColumn(name = "zone_id")
    private Zone zone;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "commercial_competences",
        joinColumns = @JoinColumn(name = "commercial_id"),
        inverseJoinColumns = @JoinColumn(name = "competence_id")
    )
    @Builder.Default
    private Set<Competence> competences = new HashSet<>();
}
