package com.smartit.smartsales.domain;

import com.smartit.smartsales.domain.enums.StatutVisite;
import com.smartit.smartsales.domain.enums.TypeVisite;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "visites")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Visite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = true)
    @JoinColumn(name = "commercial_id", nullable = true)
    private Commercial commercial;

    @ManyToOne(optional = false)
    @JoinColumn(name = "client_id")
    private Client client;

    @Column(nullable = false)
    private LocalDateTime dateVisite;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeVisite type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutVisite statut = StatutVisite.PLANIFIEE;

    @Column(columnDefinition = "TEXT")
    private String compteRendu;

    @Builder.Default
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montant = BigDecimal.ZERO;

    // Position dans la tournee optimisee par l'agent IA
    private Integer ordreTournee;

    // Check-in / check-out avec coordonnées GPS optionnelles
    private LocalDateTime checkIn;
    private LocalDateTime checkOut;
    private Double checkInLatitude;
    private Double checkInLongitude;
    private Double checkOutLatitude;
    private Double checkOutLongitude;
}
