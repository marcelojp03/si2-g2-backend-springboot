package com.uagrm.si2g2.calificacion.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "calificacion_actividad", uniqueConstraints = @UniqueConstraint(name = "uq_calificacion_actividad_estudiante", columnNames = {
        "id_actividad", "id_estudiante" }))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class CalificacionActividad {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "id_institucion", nullable = false)
    private UUID idInstitucion;

    @Column(name = "id_actividad", nullable = false)
    private UUID idActividad;

    @Column(name = "id_estudiante", nullable = false)
    private UUID idEstudiante;

    @Column(name = "nota_obtenida", precision = 5, scale = 2)
    private BigDecimal notaObtenida;

    @Column(name = "observacion", length = 500)
    private String observacion;

    @Builder.Default
    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "PENDIENTE";

    @Column(name = "id_usuario_registro")
    private UUID idUsuarioRegistro;

    @Column(name = "id_usuario_modificacion")
    private UUID idUsuarioModificacion;

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