package com.uagrm.si2g2.calificacion.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "observacion_ser", indexes = {
        @Index(name = "idx_observacion_ser_periodo_estudiante", columnList = "id_institucion, id_periodo_evaluacion, id_estudiante"),
        @Index(name = "idx_observacion_ser_fecha", columnList = "id_institucion, id_docente, fecha_observacion")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class ObservacionSer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "id_institucion", nullable = false)
    private UUID idInstitucion;

    @Column(name = "id_periodo_evaluacion", nullable = false)
    private UUID idPeriodoEvaluacion;

    @Column(name = "id_estudiante", nullable = false)
    private UUID idEstudiante;

    @Column(name = "id_docente", nullable = false)
    private UUID idDocente;

    @Column(name = "id_materia", nullable = false)
    private UUID idMateria;

    @Column(name = "fecha_observacion", nullable = false)
    private LocalDate fechaObservacion;

    @Column(name = "comportamiento", nullable = false, length = 50)
    private String comportamiento;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @PrePersist
    protected void onCreate() {
        creadoEn = Instant.now();
    }
}