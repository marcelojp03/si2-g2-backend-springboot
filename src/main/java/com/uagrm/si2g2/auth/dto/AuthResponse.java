package com.uagrm.si2g2.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AuthResponse {

    private String token;
    private String correo;
    private List<String> roles;
    private boolean requiereCambioContrasena;
}
