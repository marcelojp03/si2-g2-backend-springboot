package com.uagrm.si2g2.docente.dto;

import com.uagrm.si2g2.docente.domain.Docente;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class DocenteResponse {

    private UUID id;
    private UUID idInstitucion;
    private UUID idUsuario;
    private String codigo;
    private String documentoIdentidad;
    private String nombres;
    private String apellidos;
    private String telefono;
    private String correo;
    private String especialidad;
    private String estado;
    private Instant creadoEn;
    private Instant actualizadoEn;

    public static DocenteResponse from(Docente d) {
        return DocenteResponse.builder()
                .id(d.getId())
                .idInstitucion(d.getIdInstitucion())
                .idUsuario(d.getIdUsuario())
                .codigo(d.getCodigo())
                .documentoIdentidad(d.getDocumentoIdentidad())
                .nombres(d.getNombres())
                .apellidos(d.getApellidos())
                .telefono(d.getTelefono())
                .correo(d.getCorreo())
                .especialidad(d.getEspecialidad())
                .estado(d.getEstado())
                .creadoEn(d.getCreadoEn())
                .actualizadoEn(d.getActualizadoEn())
                .build();
    }
}
