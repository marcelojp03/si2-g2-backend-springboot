package com.uagrm.si2g2.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class PasswordRecoveryResponse {

    private String mensaje;
    private UUID challengeId;
    private String recoveryToken;
    private String codigoVerificacionDebug;
}
