package com.uagrm.si2g2.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class RegisterRequest {

    @NotBlank
    @Email
    private String correo;

    @NotBlank
    private String contrasena;

    @NotBlank
    private String nombres;

    @NotBlank
    private String apellidos;

    private UUID idInstitucion;
}
