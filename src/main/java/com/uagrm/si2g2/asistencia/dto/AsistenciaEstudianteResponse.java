package com.uagrm.si2g2.asistencia.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class AsistenciaEstudianteResponse {

    private UUID idDetalle;
    private UUID idInscripcion;
    private UUID idEstudiante;

    private String codigoEstudiante;
    private String documentoIdentidad;
    private String nombres;
    private String apellidos;
    private String nombreCompleto;

    private String estadoAsistencia;
    private boolean registrado;
}