package com.uagrm.si2g2.auth.dto;

import com.uagrm.si2g2.auth.domain.IntentoLogin;

import java.time.Instant;
import java.util.UUID;

public record IntentoLoginResponse(
        UUID id,
        String correo,
        UUID idUsuario,
        UUID idInstitucion,
        Instant fechaIntento,
        boolean exito,
        String ip,
        String motivoFallo
) {
    public static IntentoLoginResponse from(IntentoLogin i) {
        return new IntentoLoginResponse(
                i.getId(),
                i.getCorreo(),
                i.getIdUsuario(),
                i.getIdInstitucion(),
                i.getFechaIntento(),
                i.isExito(),
                i.getIp(),
                i.getMotivoFallo()
        );
    }
}
