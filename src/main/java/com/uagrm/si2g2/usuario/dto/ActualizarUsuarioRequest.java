package com.uagrm.si2g2.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ActualizarUsuarioRequest {

    @NotBlank
    private String nombres;

    @NotBlank
    private String apellidos;

    @Size(max = 30)
    private String telefono;
}
