package com.uagrm.si2g2.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.uagrm.si2g2.auth.domain.Rol;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class RolResponse {

    private UUID id;
    private String codigo;
    private String nombre;
    private String descripcion;
    private UUID idInstitucion;
    @JsonProperty("esGlobal")
    private boolean esGlobal;
    @JsonProperty("editable")
    private boolean editable;
    private List<PermisoResponse> permisos;

    public static RolResponse from(Rol rol, boolean editable) {
        return RolResponse.builder()
                .id(rol.getId())
                .codigo(rol.getCodigo())
                .nombre(rol.getNombre())
                .descripcion(rol.getDescripcion())
                .idInstitucion(rol.getIdInstitucion())
                .esGlobal(rol.isEsGlobal())
                .editable(editable)
                .permisos(rol.getPermisos().stream().map(PermisoResponse::from).toList())
                .build();
    }
}
