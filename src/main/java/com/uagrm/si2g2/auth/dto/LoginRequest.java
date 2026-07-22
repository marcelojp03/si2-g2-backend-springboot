package com.uagrm.si2g2.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Locale;

@Data
public class LoginRequest {

    @NotBlank
    @Email
    private String correo;

    public void setCorreo(String correo) {
        this.correo = correo == null ? null : correo.trim().toLowerCase(Locale.ROOT);
    }

    @NotBlank
    private String contrasena;
}
