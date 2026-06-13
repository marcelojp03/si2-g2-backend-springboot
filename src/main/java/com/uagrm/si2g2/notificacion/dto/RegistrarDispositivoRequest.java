package com.uagrm.si2g2.notificacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrarDispositivoRequest(
        @NotBlank @Size(max = 512) String tokenDispositivo,
        @NotBlank @Size(max = 20) String plataforma
) {}
