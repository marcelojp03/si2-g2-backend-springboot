package com.uagrm.si2g2.calificacion.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "autoevaluacion_trimestral", uniqueConstraints = @UniqueConstraint(name = "uq_autoevaluacion_trimestral", columnNames = {
        "id_estudiante", "id_materia", "id_trimestre", "id_gestion_academica" }))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class AutoevaluacionTrimestral {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "id_institucion", nullable = false)
    private UUID idInstitucion;

    @Column(name = "id_trimestre", nullable = false)
    private UUID idTrimestre;

    @Column(name = "id_gestion_academica", nullable = false)
    private UUID idGestionAcademica;

    @Column(name = "id_materia", nullable = false)
    private UUID idMateria;

    @Column(name = "id_estudiante", nullable = false)
    private UUID idEstudiante;

    @Column(name = "nota_autoevaluacion", nullable = false, precision = 5, scale = 2)
    private BigDecimal notaAutoevaluacion;

    @Column(name = "comentario", length = 1000)
    private String comentario;

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