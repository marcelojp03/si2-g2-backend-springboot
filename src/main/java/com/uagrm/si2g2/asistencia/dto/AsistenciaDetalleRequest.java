package com.uagrm.si2g2.asistencia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.UUID;

@Data
public class AsistenciaDetalleRequest {

    @NotNull
    private UUID idInscripcion;

    @NotBlank
    @Pattern(
            regexp = "PRESENTE|AUSENTE|TARDANZA|JUSTIFICADO",
            message = "El estado debe ser PRESENTE, AUSENTE, TARDANZA o JUSTIFICADO"
    )
    private String estadoAsistencia;
}