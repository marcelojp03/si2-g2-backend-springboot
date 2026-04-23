package com.uagrm.si2g2.asignacion.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AsignacionDocenteRequest {

    @NotNull
    private UUID idDocente;

    @NotNull
    private UUID idMateria;

    @NotNull
    private UUID idParalelo;

    @NotNull
    private UUID idGestion;
}
