package com.uagrm.si2g2.tutor.dto;

import com.uagrm.si2g2.tutor.domain.Tutor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class TutorResponse {

    private UUID id;
    private UUID idInstitucion;
    private String documentoIdentidad;
    private String nombres;
    private String apellidos;
    private String telefono;
    private String correo;
    private String estado;
    private Instant creadoEn;
    private Instant actualizadoEn;

    public static TutorResponse from(Tutor t) {
        return TutorResponse.builder()
                .id(t.getId())
                .idInstitucion(t.getIdInstitucion())
                .documentoIdentidad(t.getDocumentoIdentidad())
                .nombres(t.getNombres())
                .apellidos(t.getApellidos())
                .telefono(t.getTelefono())
                .correo(t.getCorreo())
                .estado(t.getEstado())
                .creadoEn(t.getCreadoEn())
                .actualizadoEn(t.getActualizadoEn())
                .build();
    }
}
