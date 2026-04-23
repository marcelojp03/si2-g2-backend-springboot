package com.uagrm.si2g2.curso.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CursoRequest {

    @NotBlank
    @Size(max = 100)
    private String nombre;

    @Size(max = 50)
    private String nivel;

    private String descripcion;
}
