package com.uagrm.si2g2.aula.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class AulaRequest {

    @NotBlank
    @Size(max = 30)
    private String codigo;

    @NotBlank
    @Size(max = 120)
    private String nombre;

    @NotNull
    @Min(1)
    private Integer capacidad;

    @Size(max = 180)
    private String ubicacion;

    private List<@Size(max = 60) String> recursos;
}
