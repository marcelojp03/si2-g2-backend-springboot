package com.uagrm.si2g2.estudiante.dto;

import java.util.List;
import java.util.UUID;

public record HistorialAcademicoResponse(
        UUID idEstudiante,
        String codigoEstudiante,
        String nombres,
        String apellidos,
        List<HistorialGestionResponse> gestiones
) {}
