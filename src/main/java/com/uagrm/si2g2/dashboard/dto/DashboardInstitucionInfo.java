package com.uagrm.si2g2.dashboard.dto;

import java.util.UUID;

public record DashboardInstitucionInfo(
        UUID id,
        String codigo,
        String nombre,
        String tipoInstitucion,
        String telefono,
        String correo,
        String direccion,
        String estado,
        String nombreCorto,
        String colorPrimario,
        String logoUrl
) {
}
