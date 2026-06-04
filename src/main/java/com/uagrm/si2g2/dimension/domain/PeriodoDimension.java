package com.uagrm.si2g2.dimension.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "periodo_dimension", schema = "sia",
        uniqueConstraints = @UniqueConstraint(name = "uq_periodo_dimension",
                columnNames = {"id_periodo_evaluacion", "id_dimension"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class PeriodoDimension {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "id_periodo_evaluacion", nullable = false)
    private UUID idPeriodoEvaluacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_dimension", nullable = false)
    private Dimension dimension;

    @Column(name = "ponderacion", nullable = false)
    private Integer ponderacion;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @PrePersist
    void onCreate() {
        creadoEn = Instant.now();
        actualizadoEn = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        actualizadoEn = Instant.now();
    }
}
