package com.uagrm.si2g2.pagos.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class GenerarQrResponse {
    private UUID idPago;
    private String qrBase64;
    private String proveedor;
    private String referenciaExterna;
}
