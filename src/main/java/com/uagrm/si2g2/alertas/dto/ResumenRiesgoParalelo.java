package com.uagrm.si2g2.alertas.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ResumenRiesgoParalelo(
        UUID idParalelo,
        String nombreParalelo,
        int totalEstudiantes,
        int totalConDatos,
        int totalSinDatos,
        int estudiantesEnRiesgo,
        BigDecimal porcentajeRiesgo,
        BigDecimal scorePromedio
) {}
