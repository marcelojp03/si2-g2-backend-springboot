package com.uagrm.si2g2.calificacion.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CalificacionDetalleRequest {
    private UUID idInscripcion;
    private BigDecimal notaNumerica;

    @Size(max = 5)
    private String notaLiteral;

    @Size(max = 255)
    private String razonCambio;
}
