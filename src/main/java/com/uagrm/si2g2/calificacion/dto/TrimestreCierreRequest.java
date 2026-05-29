package com.uagrm.si2g2.calificacion.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TrimestreCierreRequest(
        @NotNull UUID idGestionAcademica,
        @NotNull Integer trimestre,
        String justificacion) {
}