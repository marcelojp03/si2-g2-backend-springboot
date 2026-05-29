package com.uagrm.si2g2.calificacion.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ConsolidadoTrimestralMateriaResponse(
        UUID idMateria,
        String codigoMateria,
        String nombreMateria,
        UUID idDocente,
        String nombreDocente,
        int actividadesSaber,
        int actividadesHacer,
        int estudiantesPendientesAutoevaluacion,
        BigDecimal promedioGeneral,
        String estado,
        List<ConsolidadoTrimestralEstudianteResponse> estudiantes) {
}