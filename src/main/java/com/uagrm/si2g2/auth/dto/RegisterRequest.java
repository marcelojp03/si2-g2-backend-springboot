package com.uagrm.si2g2.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class RegisterRequest {

    @NotBlank
    @Email
    private String correo;

    @NotBlank
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String contrasena;

    @NotBlank
    private String nombres;

    @NotBlank
    private String apellidos;

    private UUID idInstitucion;

    /** Opcional. Si no se envía, se asigna ADMIN_INSTITUCION por defecto. */
    private String codigoRol;
}
