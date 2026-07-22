package com.uagrm.si2g2.alertas.dto;

import com.uagrm.si2g2.alertas.domain.AlertaRiesgoSeguimiento;

import java.time.Instant;
import java.util.UUID;

public record AlertaRiesgoSeguimientoResponse(
        UUID id,
        UUID idAlertaRiesgo,
        String estadoAnterior,
        String estadoNuevo,
        String observacion,
        UUID idUsuario,
        String usuario,
        Instant creadoEn
) {
    public static AlertaRiesgoSeguimientoResponse from(AlertaRiesgoSeguimiento seguimiento, String usuario) {
        return new AlertaRiesgoSeguimientoResponse(seguimiento.getId(), seguimiento.getIdAlertaRiesgo(),
                seguimiento.getEstadoAnterior(), seguimiento.getEstadoNuevo(), seguimiento.getObservacion(),
                seguimiento.getIdUsuario(), usuario, seguimiento.getCreadoEn());
    }
}
