package com.uagrm.si2g2.calificacion.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "actividad_evaluativa", uniqueConstraints = @UniqueConstraint(name = "uq_actividad_evaluativa_periodo_nombre", columnNames = {
        "id_institucion", "id_periodo_trimestral", "nombre_actividad" }))
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

    @Column(name = "id_periodo_trimestral", nullable = false)
    private UUID idPeriodoTrimestral;

    @Column(name = "id_gestion_academica", nullable = false)
    private UUID idGestionAcademica;

    @Column(name = "id_curso", nullable = false)
    private UUID idCurso;

    @Column(name = "id_paralelo", nullable = false)
    private UUID idParalelo;

    @Column(name = "id_materia", nullable = false)
    private UUID idMateria;

    @Column(name = "id_docente", nullable = false)
    private UUID idDocente;

    @Column(name = "nombre_actividad", nullable = false, length = 150)
    private String nombreActividad;

    @Column(name = "tipo_actividad", nullable = false, length = 30)
    private String tipoActividad;

    @Column(name = "dimension", nullable = false, length = 15)
    private String dimension;

    @Column(name = "puntaje_maximo", nullable = false)
    private Integer puntajeMaximo;

    @Column(name = "fecha_actividad", nullable = false)
    private Instant fechaActividad;

    @Column(name = "descripcion", length = 1000)
    private String descripcion;

    @Builder.Default
    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "BORRADOR";

    @Column(name = "publicado_en")
    private Instant publicadoEn;

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