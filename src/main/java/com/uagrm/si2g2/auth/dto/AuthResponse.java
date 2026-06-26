package com.uagrm.si2g2.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AuthResponse {

    private String token;
    private String id;
    private String nombres;
    private String apellidos;
    private String correo;
    private String idInstitucion;
    private List<String> roles;
    private List<String> permisos;
    private String idEstudiante;
    private String idTutor;
    private boolean requiereCambioContrasena;
}
