package com.uagrm.si2g2.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class PasswordRecoveryVerifyRequest {

    private UUID challengeId;

    @NotBlank
    private String codigoVerificacion;
}
