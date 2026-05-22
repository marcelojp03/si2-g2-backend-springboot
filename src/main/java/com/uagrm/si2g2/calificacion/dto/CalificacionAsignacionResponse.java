package com.uagrm.si2g2.calificacion.dto;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class CalificacionAsignacionResponse {
    UUID idAsignacionDocente;
    UUID idDocente;
    String codigoDocente;
    String nombreDocente;
    UUID idMateria;
    String codigoMateria;
    String nombreMateria;
    UUID idParalelo;
    String nombreParalelo;
    UUID idCurso;
    String nombreCurso;
    UUID idGestion;
    String nombreGestion;
    String estado;
}
