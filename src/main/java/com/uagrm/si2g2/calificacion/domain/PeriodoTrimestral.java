package com.uagrm.si2g2.calificacion.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "periodo_trimestral", uniqueConstraints = @UniqueConstraint(name = "uq_periodo_trimestral_gestion_numero", columnNames = {
        "id_institucion", "id_gestion_academica", "numero_trimestre" }))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class PeriodoTrimestral {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "id_institucion", nullable = false)
    private UUID idInstitucion;

    @Column(name = "id_gestion_academica", nullable = false)
    private UUID idGestionAcademica;

    @Column(name = "numero_trimestre", nullable = false)
    private Integer numeroTrimestre;

    @Builder.Default
    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "ABIERTO";

    @Column(name = "fecha_cierre")
    private Instant fechaCierre;

    @Column(name = "justificacion_cierre", length = 500)
    private String justificacionCierre;

    @Column(name = "id_usuario_cierre")
    private UUID idUsuarioCierre;

    @Column(name = "fecha_reapertura")
    private Instant fechaReapertura;

    @Column(name = "justificacion_reapertura", length = 500)
    private String justificacionReapertura;

    @Column(name = "id_usuario_reapertura")
    private UUID idUsuarioReapertura;

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