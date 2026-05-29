package com.uagrm.si2g2.calificacion.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CalificacionSerRequest(
        @NotNull UUID idGestionAcademica,
        @NotNull Integer trimestre,
        @NotNull UUID idCurso,
        @NotNull UUID idParalelo,
        @NotNull UUID idMateria,
        @NotNull UUID idDocente,
        @NotNull UUID idEstudiante,
        @NotNull BigDecimal notaSer,
        String observacion) {
}