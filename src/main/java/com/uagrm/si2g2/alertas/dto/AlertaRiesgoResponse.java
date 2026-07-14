package com.uagrm.si2g2.alertas.dto;

import com.uagrm.si2g2.alertas.domain.AlertaRiesgo;
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
    private String estadoAlerta;
    private Boolean activa;
    private Instant procesadoEn;
    private Instant creadoEn;

    public static AlertaRiesgoResponse from(AlertaRiesgo a) {
        return AlertaRiesgoResponse.builder()
                .id(a.getId())
                .idInstitucion(a.getIdInstitucion())
                .idEstudiante(a.getIdEstudiante())
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
                .estadoAlerta(a.getEstadoAlerta())
                .activa(a.getActiva())
                .procesadoEn(a.getProcesadoEn())
                .creadoEn(a.getCreadoEn())
                .build();
    }
}
