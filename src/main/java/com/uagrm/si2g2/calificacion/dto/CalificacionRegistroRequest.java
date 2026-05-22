package com.uagrm.si2g2.calificacion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CalificacionRegistroRequest {

    @NotNull
    private UUID idEvaluacion;

    @Valid
    @NotEmpty
    private List<CalificacionDetalleRequest> detalles;
}
