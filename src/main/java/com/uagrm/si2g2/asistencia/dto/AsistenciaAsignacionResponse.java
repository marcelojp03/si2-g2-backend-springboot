package com.uagrm.si2g2.asistencia.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class AsistenciaAsignacionResponse {

    private UUID idAsignacionDocente;

    private UUID idDocente;
    private String codigoDocente;
    private String nombreDocente;

    private UUID idMateria;
    private String codigoMateria;
    private String nombreMateria;

    private UUID idParalelo;
    private String nombreParalelo;

    private UUID idCurso;
    private String nombreCurso;

    private UUID idGestion;
    private String nombreGestion;

    private String estado;
}