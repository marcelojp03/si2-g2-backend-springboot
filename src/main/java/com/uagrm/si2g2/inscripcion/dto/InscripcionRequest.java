package com.uagrm.si2g2.inscripcion.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class InscripcionRequest {

    @NotNull
    private UUID idEstudiante;

    @NotNull
    private UUID idGestion;

    @NotNull
    private UUID idCurso;

    @NotNull
    private UUID idParalelo;

    private LocalDate fechaInscripcion;
}
