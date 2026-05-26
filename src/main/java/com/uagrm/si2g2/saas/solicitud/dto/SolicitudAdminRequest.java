package com.uagrm.si2g2.saas.solicitud.dto;

import jakarta.validation.constraints.Size;

public record SolicitudAdminRequest(
        @Size(max = 2000) String notasAdmin
) {}
