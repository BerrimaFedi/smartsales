package com.smartit.smartsales.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "performances")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Performance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "commercial_id")
    private Commercial commercial;

    // Periode au format "AAAA-MM", ex : "2026-06"
    @Column(nullable = false)
    private String periode;

    @Builder.Default
    private BigDecimal chiffreAffaires = BigDecimal.ZERO;

    @Builder.Default
    private int nombreVisites = 0;

    // Taux de conversion en pourcentage (0-100)
    @Builder.Default
    private double tauxConversion = 0.0;
}
