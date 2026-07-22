package com.uagrm.si2g2.alertas.dto;

import com.uagrm.si2g2.alertas.domain.AlertaRiesgo;
import com.uagrm.si2g2.estudiante.domain.Estudiante;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class AlertaRiesgoResponse {

    private UUID id;
    private UUID idInstitucion;
    private UUID idEstudiante;
    private String codigoEstudiante;
    private String nombreEstudiante;
    private UUID idGestionAcademica;
    private String nivelRiesgo;
    private String motivo;
    private BigDecimal scoreIa;
    private BigDecimal porcentajeAsistencia;
    private BigDecimal promedioCalificaciones;
    private String tendenciaNotas;
    private Integer evaluacionesPendientes;
    private Integer materiasReprobadasHistorial;
    private String factoresJson;
    private Boolean datosVigentes;
    private Instant ultimaEvaluacionValidaEn;
    private String estadoAlerta;
    private Boolean activa;
    private Instant procesadoEn;
    private Instant creadoEn;

    public static AlertaRiesgoResponse from(AlertaRiesgo a) {
        return from(a, null);
    }

    public static AlertaRiesgoResponse from(AlertaRiesgo a, Estudiante estudiante) {
        return AlertaRiesgoResponse.builder()
                .id(a.getId())
                .idInstitucion(a.getIdInstitucion())
                .idEstudiante(a.getIdEstudiante())
                .codigoEstudiante(estudiante == null ? null : estudiante.getCodigoEstudiante())
                .nombreEstudiante(estudiante == null ? null
                        : estudiante.getNombres() + " " + estudiante.getApellidos())
                .idGestionAcademica(a.getIdGestionAcademica())
                .nivelRiesgo(a.getNivelRiesgo())
                .motivo(a.getMotivo())
                .scoreIa(a.getScoreIa())
                .porcentajeAsistencia(a.getPorcentajeAsistencia())
                .promedioCalificaciones(a.getPromedioCalificaciones())
                .tendenciaNotas(a.getTendenciaNotas())
                .evaluacionesPendientes(a.getEvaluacionesPendientes())
                .materiasReprobadasHistorial(a.getMateriasReprobadasHistorial())
                .factoresJson(a.getFactoresJson())
                .datosVigentes(a.getDatosVigentes())
                .ultimaEvaluacionValidaEn(a.getUltimaEvaluacionValidaEn())
                .estadoAlerta(a.getEstadoAlerta())
                .activa(a.getActiva())
                .procesadoEn(a.getProcesadoEn())
                .creadoEn(a.getCreadoEn())
                .build();
    }
}
