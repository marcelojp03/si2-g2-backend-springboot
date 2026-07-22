package com.uagrm.si2g2.calificacion.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "actividad_evaluativa", uniqueConstraints = @UniqueConstraint(name = "uq_actividad_evaluativa_periodo_nombre", columnNames = {
        "id_institucion", "id_periodo_evaluacion", "id_materia", "nombre_actividad" }))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class ActividadEvaluativa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "id_institucion", nullable = false)
    private UUID idInstitucion;

    @Column(name = "id_periodo_evaluacion", nullable = false)
    private UUID idPeriodoEvaluacion;

    @Column(name = "id_materia", nullable = false)
    private UUID idMateria;

    @Column(name = "id_docente", nullable = false)
    private UUID idDocente;

    @Column(name = "nombre_actividad", nullable = false, length = 150)
    private String nombreActividad;

    @Column(name = "dimension", nullable = false, length = 15)
    private String dimension;

    @Column(name = "fecha_actividad", nullable = false)
    private LocalDate fechaActividad;

    @Column(name = "descripcion_evidencia", columnDefinition = "TEXT")
    private String descripcionEvidencia;

    @Builder.Default
    @Column(name = "puntaje_maximo", nullable = false)
    private Integer puntajeMaximo = 100;

    @Builder.Default
    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "BORRADOR";

    @Column(name = "publicado_en")
    private Instant publicadoEn;

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
