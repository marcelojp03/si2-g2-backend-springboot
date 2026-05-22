package com.uagrm.si2g2.calificacion.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

@Value
@Builder
public class CalificacionEstudianteResponse {
    UUID idCalificacion;
    UUID idInscripcion;
    UUID idEstudiante;
    String codigoEstudiante;
    String documentoIdentidad;
    String nombres;
    String apellidos;
    String nombreCompleto;
    BigDecimal notaNumerica;
    String notaLiteral;
    boolean registrado;
}
