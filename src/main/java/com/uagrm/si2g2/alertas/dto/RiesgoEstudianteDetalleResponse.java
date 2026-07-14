package com.uagrm.si2g2.alertas.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RiesgoEstudianteDetalleResponse(
        UUID idAlerta,
        UUID idEstudiante,
        String codigoEstudiante,
        String nombres,
        String apellidos,
        String nombreCurso,
        String nombreParalelo,
        BigDecimal score,
        String nivelRiesgo,
        BigDecimal porcentajeAsistencia,
        BigDecimal promedioCalificaciones,
        String tendenciaNotas,
        int evaluacionesPendientes,
        int materiasReprobadasHistorial,
        String estadoAlerta,
        List<FactorContribuyente> factores,
        List<String> recomendaciones,
        List<EvolucionNotaResponse> evolucionNotas
) {}
