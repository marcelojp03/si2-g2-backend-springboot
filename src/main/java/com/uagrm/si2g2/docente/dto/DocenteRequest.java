package com.uagrm.si2g2.docente.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DocenteRequest {

    @NotBlank
    @Size(max = 30)
    private String codigo;

    @NotBlank
    @Size(max = 20)
    private String documentoIdentidad;

    @NotBlank
    @Size(max = 100)
    private String nombres;

    @NotBlank
    @Size(max = 100)
    private String apellidos;

    @Size(max = 20)
    private String telefono;

    @NotBlank
    @Email
    @Size(max = 150)
    private String correo;

    @Size(max = 150)
    private String especialidad;
}
