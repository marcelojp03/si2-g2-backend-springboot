package com.uagrm.si2g2.notificacion.dto;

import com.uagrm.si2g2.notificacion.domain.Notificacion;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class NotificacionResponse {

    private UUID id;
    private UUID idInstitucion;
    private UUID idUsuario;
    private String titulo;
    private String mensaje;
    private String tipo;
    private String referenciaTipo;
    private UUID referenciaId;
    private Boolean leida;
    private Instant leidaEn;
    private Instant creadoEn;

    public static NotificacionResponse from(Notificacion n) {
        return NotificacionResponse.builder()
                .id(n.getId())
                .idInstitucion(n.getIdInstitucion())
                .idUsuario(n.getIdUsuario())
                .titulo(n.getTitulo())
                .mensaje(n.getMensaje())
                .tipo(n.getTipo())
                .referenciaTipo(n.getReferenciaTipo())
                .referenciaId(n.getReferenciaId())
                .leida(n.getLeida())
                .leidaEn(n.getLeidaEn())
                .creadoEn(n.getCreadoEn())
                .build();
    }
}
