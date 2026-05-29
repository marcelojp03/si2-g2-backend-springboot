package com.uagrm.si2g2.calificacion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CalificacionActividadRegistroRequest(
        @NotNull UUID idActividad,
        @Valid @NotNull List<CalificacionActividadDetalleRequest> detalles) {
}