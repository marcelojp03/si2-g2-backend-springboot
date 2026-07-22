package com.uagrm.si2g2.alertas.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alerta_riesgo")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class AlertaRiesgo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "id_institucion", nullable = false)
    private UUID idInstitucion;

    @Column(name = "id_estudiante", nullable = false)
    private UUID idEstudiante;

    @Column(name = "id_gestion_academica", nullable = false)
    private UUID idGestionAcademica;

    @Column(name = "nivel_riesgo", nullable = false, length = 20)
    private String nivelRiesgo;

    @Column(name = "motivo", columnDefinition = "TEXT")
    private String motivo;

    @Column(name = "score_ia", precision = 5, scale = 4)
    private BigDecimal scoreIa;

    @Column(name = "porcentaje_asistencia", precision = 5, scale = 2)
    private BigDecimal porcentajeAsistencia;

    @Column(name = "promedio_calificaciones", precision = 5, scale = 2)
    private BigDecimal promedioCalificaciones;

    @Column(name = "tendencia_notas", length = 20)
    private String tendenciaNotas;

    @Builder.Default
    @Column(name = "evaluaciones_pendientes", nullable = false)
    private Integer evaluacionesPendientes = 0;

    @Builder.Default
    @Column(name = "materias_reprobadas_historial", nullable = false)
    private Integer materiasReprobadasHistorial = 0;

    @Column(name = "factores_json", columnDefinition = "TEXT")
    private String factoresJson;

    @Builder.Default
    @Column(name = "datos_vigentes", nullable = false)
    private Boolean datosVigentes = true;

    @Column(name = "ultima_evaluacion_valida_en")
    private Instant ultimaEvaluacionValidaEn;

    @Builder.Default
    @Column(name = "estado_alerta", nullable = false, length = 20)
    private String estadoAlerta = "ABIERTA";

    @Builder.Default
    @Column(name = "activa", nullable = false)
    private Boolean activa = true;

    @Column(name = "procesado_en", nullable = false)
    private Instant procesadoEn;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        procesadoEn = now;
        creadoEn = now;
        actualizadoEn = now;
    }

    @PreUpdate
    protected void onUpdate() {
        actualizadoEn = Instant.now();
    }
}
