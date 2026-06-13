package com.uagrm.si2g2.pagos.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class CuotaEstudianteResponse {

    private UUID id;
    private UUID idInstitucion;
    private UUID idEstudiante;
    private UUID idPlanPago;
    private String nombrePlan;
    private UUID idGestionAcademica;
    private Integer numeroCuota;
    private BigDecimal monto;
    private LocalDate fechaVencimiento;
    private String estado;
    private Instant creadoEn;
    private PagoResponse ultimoPago;
}
