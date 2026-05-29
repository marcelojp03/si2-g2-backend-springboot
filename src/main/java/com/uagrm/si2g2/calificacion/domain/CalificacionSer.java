package com.uagrm.si2g2.calificacion.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "calificacion_ser", uniqueConstraints = @UniqueConstraint(name = "uq_calificacion_ser_periodo_estudiante", columnNames = {
        "id_estudiante", "id_materia", "id_trimestre", "id_gestion_academica" }))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class CalificacionSer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "id_institucion", nullable = false)
    private UUID idInstitucion;

    @Column(name = "id_trimestre", nullable = false)
    private UUID idTrimestre;

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

    @Column(name = "id_estudiante", nullable = false)
    private UUID idEstudiante;

    @Column(name = "nota_ser", nullable = false, precision = 5, scale = 2)
    private BigDecimal notaSer;

    @Column(name = "observacion", length = 500)
    private String observacion;

    @Builder.Default
    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "REGISTRADA";

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