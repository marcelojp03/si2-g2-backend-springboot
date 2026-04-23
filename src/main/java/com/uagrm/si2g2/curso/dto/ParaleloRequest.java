package com.uagrm.si2g2.curso.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class ParaleloRequest {

    @NotNull
    private UUID idCurso;

    @NotNull
    private UUID idGestionAcademica;

    @NotBlank
    @Size(max = 20)
    private String nombre;

    @Min(1)
    private Integer capacidad;
}
