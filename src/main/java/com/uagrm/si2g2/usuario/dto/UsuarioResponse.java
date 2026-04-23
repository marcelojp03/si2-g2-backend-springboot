package com.uagrm.si2g2.usuario.dto;

import com.uagrm.si2g2.auth.domain.Rol;
import com.uagrm.si2g2.auth.domain.Usuario;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Builder
public class UsuarioResponse {

    private UUID id;
    private UUID idInstitucion;
    private String correo;
    private String nombres;
    private String apellidos;
    private String telefono;
    private String estado;
    private List<String> roles;
    private Instant ultimoAcceso;
    private Instant creadoEn;
    private Instant actualizadoEn;

    public static UsuarioResponse from(Usuario u) {
        return UsuarioResponse.builder()
                .id(u.getId())
                .idInstitucion(u.getIdInstitucion())
                .correo(u.getCorreo())
                .nombres(u.getNombres())
                .apellidos(u.getApellidos())
                .telefono(u.getTelefono())
                .estado(u.getEstado())
                .roles(u.getRoles().stream().map(Rol::getCodigo).collect(Collectors.toList()))
                .ultimoAcceso(u.getUltimoAcceso())
                .creadoEn(u.getCreadoEn())
                .actualizadoEn(u.getActualizadoEn())
                .build();
    }
}
