package com.uagrm.si2g2.calificacion.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "evaluacion")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Evaluacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "id_institucion", nullable = false)
    private UUID idInstitucion;

    @Column(name = "id_asignacion_docente", nullable = false)
    private UUID idAsignacionDocente;

    @Column(name = "creado_por")
    private UUID creadoPor;

    @Column(name = "periodo", nullable = false)
    private Integer periodo;

    @Column(name = "tipo", nullable = false, length = 40)
    private String tipo;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "ponderacion", nullable = false, precision = 5, scale = 2)
    private BigDecimal ponderacion;

    @Builder.Default
    @Column(name = "escala", nullable = false, length = 15)
    private String escala = "NUMERICA";

    @Builder.Default
    @Column(name = "estado", nullable = false, length = 15)
    private String estado = "ABIERTA";

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @PrePersist
    protected void onCreate() {
        creadoEn = Instant.now();
        actualizadoEn = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        actualizadoEn = Instant.now();
    }
}
