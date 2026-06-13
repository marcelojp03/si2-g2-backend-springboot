package com.uagrm.si2g2.pagos.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class GenerarCuotasRequest {

    @NotNull
    private UUID idPlanPago;

    @NotNull
    private UUID idGestionAcademica;

    private UUID idEstudiante;
}
