package com.uagrm.si2g2.comunicado.dto;

import com.uagrm.si2g2.comunicado.domain.Comunicado;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class ComunicadoResponse {

    private UUID id;
    private UUID idInstitucion;
    private String titulo;
    private String contenido;
    private String tipo;
    private String destinatarios;
    private String estado;
    private Instant publicadoEn;
    private UUID publicadoPor;
    private UUID creadoPor;
    private Instant creadoEn;
    private Instant actualizadoEn;

    public static ComunicadoResponse from(Comunicado c) {
        return ComunicadoResponse.builder()
                .id(c.getId())
                .idInstitucion(c.getIdInstitucion())
                .titulo(c.getTitulo())
                .contenido(c.getContenido())
                .tipo(c.getTipo())
                .destinatarios(c.getDestinatarios())
                .estado(c.getEstado())
                .publicadoEn(c.getPublicadoEn())
                .publicadoPor(c.getPublicadoPor())
                .creadoPor(c.getCreadoPor())
                .creadoEn(c.getCreadoEn())
                .actualizadoEn(c.getActualizadoEn())
                .build();
    }
}
