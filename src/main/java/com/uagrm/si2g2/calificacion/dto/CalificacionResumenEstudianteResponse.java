package com.uagrm.si2g2.calificacion.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

@Value
@Builder
public class CalificacionResumenEstudianteResponse {
    UUID idInscripcion;
    UUID idEstudiante;
    String codigoEstudiante;
    String nombreCompleto;
    BigDecimal notaConsolidada;
    BigDecimal ponderacionRegistrada;
    String estadoAcademico;
}
