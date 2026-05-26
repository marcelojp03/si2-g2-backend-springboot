package com.uagrm.si2g2.ia.dto;

import jakarta.validation.constraints.NotBlank;

public record InterpretacionIaRequest(
        @NotBlank String texto,   // consulta en lenguaje natural
        @NotBlank String entidad  // "asistencia" | "calificacion" | "inscripcion"
) {}
