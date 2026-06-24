package com.uagrm.si2g2.pagos.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class EstadoPagoResponse {
    private UUID idPago;
    private String referenciaExterna;
    private String estadoVpay;
    private boolean pagado;
    private String estadoPago;
}
