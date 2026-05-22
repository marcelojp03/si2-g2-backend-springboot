package com.uagrm.si2g2.asistencia.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class AsistenciaRegistroRequest {

    @NotNull
    private UUID idAsignacionDocente;

    @NotNull
    private LocalDate fecha;

    @NotEmpty
    private List<@Valid AsistenciaDetalleRequest> detalles;
}