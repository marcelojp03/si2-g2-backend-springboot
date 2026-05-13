package com.uagrm.si2g2.auth.dto;

import com.uagrm.si2g2.auth.domain.Permiso;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class PermisoResponse {

    private UUID id;
    private String codigo;
    private String nombre;
    private String modulo;
    private String accion;
    private String descripcion;

    public static PermisoResponse from(Permiso permiso) {
        return PermisoResponse.builder()
                .id(permiso.getId())
                .codigo(permiso.getCodigo())
                .nombre(permiso.getNombre())
                .modulo(permiso.getModulo())
                .accion(permiso.getAccion())
                .descripcion(permiso.getDescripcion())
                .build();
    }
}
