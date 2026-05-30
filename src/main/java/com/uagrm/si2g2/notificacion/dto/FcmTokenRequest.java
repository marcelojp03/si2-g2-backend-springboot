package com.uagrm.si2g2.notificacion.dto;

import jakarta.validation.constraints.NotBlank;

public record FcmTokenRequest(
        @NotBlank String fcmToken,
        String plataforma    // "android" | "ios" (opcional, informativo)
) {}
