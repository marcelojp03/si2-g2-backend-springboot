package com.uagrm.si2g2.dashboard.dto;

import com.uagrm.si2g2.alertas.domain.AlertaRiesgo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DashboardRiesgoAcademicoAlert(
        UUID idAlerta,
        UUID idEstudiante,
        String nombreEstudiante,
        String nivelRiesgo,
        BigDecimal score,
        String estado,
        Instant procesadoEn,
        Boolean datosVigentes,
        Instant ultimaEvaluacionValidaEn
) {
    public static DashboardRiesgoAcademicoAlert from(AlertaRiesgo alerta, String nombreEstudiante) {
        BigDecimal score = alerta.getScoreIa() == null ? BigDecimal.ZERO
                : alerta.getScoreIa().multiply(BigDecimal.valueOf(100));
        return new DashboardRiesgoAcademicoAlert(alerta.getId(), alerta.getIdEstudiante(), nombreEstudiante,
                alerta.getNivelRiesgo(), score, alerta.getEstadoAlerta(), alerta.getProcesadoEn(),
                alerta.getDatosVigentes(), alerta.getUltimaEvaluacionValidaEn());
    }
}
