package com.uagrm.si2g2.institucion.dto;

import com.uagrm.si2g2.institucion.domain.Institucion;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class InstitucionResponse {

    private UUID id;
    private String codigo;
    private String nombre;
    private String tipoInstitucion;
    private String telefono;
    private String correo;
    private String direccion;
    private String estado;
    private Instant creadoEn;
    private Instant actualizadoEn;

    public static InstitucionResponse from(Institucion i) {
        return InstitucionResponse.builder()
                .id(i.getId())
                .codigo(i.getCodigo())
                .nombre(i.getNombre())
                .tipoInstitucion(i.getTipoInstitucion())
                .telefono(i.getTelefono())
                .correo(i.getCorreo())
                .direccion(i.getDireccion())
                .estado(i.getEstado())
                .creadoEn(i.getCreadoEn())
                .actualizadoEn(i.getActualizadoEn())
                .build();
    }
}
