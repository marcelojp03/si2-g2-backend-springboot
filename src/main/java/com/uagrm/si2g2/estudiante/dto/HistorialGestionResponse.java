package com.uagrm.si2g2.estudiante.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record HistorialGestionResponse(
        UUID idGestion,
        String nombreGestion,
        UUID idParalelo,
        String nombreParalelo,
        UUID idInscripcion,
        String estadoInscripcion,
        LocalDate fechaInscripcion,
        List<HistorialMateriaResponse> materias
) {}
