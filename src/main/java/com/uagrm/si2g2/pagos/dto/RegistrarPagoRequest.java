package com.uagrm.si2g2.pagos.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class RegistrarPagoRequest {

    @NotNull
    private UUID idCuota;

    @NotNull
    private BigDecimal monto;

    private String metodoPago;

    private String proveedor;

    private String referenciaExterna;

    private String qrBase64;
}
