package com.uagrm.si2g2.saas.solicitud.dto;

import com.uagrm.si2g2.saas.solicitud.domain.SolicitudOnboarding;

import java.time.Instant;
import java.util.UUID;

public record SolicitudOnboardingResponse(
        UUID id,
        String nombreInstitucion,
        String tipoInstitucion,
        String telefonoInstitucion,
        String correoInstitucion,
        String direccionInstitucion,
        String nombresContacto,
        String apellidosContacto,
        String correoContacto,
        String telefonoContacto,
        UUID idPlan,
        String nombrePlan,
        String mensaje,
        String estado,
        String notasAdmin,
        UUID idInstitucionCreada,
        UUID idUsuarioCreado,
        Instant creadoEn,
        Instant actualizadoEn
) {
    public static SolicitudOnboardingResponse from(SolicitudOnboarding s) {
        return new SolicitudOnboardingResponse(
                s.getId(),
                s.getNombreInstitucion(),
                s.getTipoInstitucion(),
                s.getTelefonoInstitucion(),
                s.getCorreoInstitucion(),
                s.getDireccionInstitucion(),
                s.getNombresContacto(),
                s.getApellidosContacto(),
                s.getCorreoContacto(),
                s.getTelefonoContacto(),
                s.getPlan() != null ? s.getPlan().getId() : null,
                s.getPlan() != null ? s.getPlan().getNombre() : null,
                s.getMensaje(),
                s.getEstado(),
                s.getNotasAdmin(),
                s.getIdInstitucionCreada(),
                s.getIdUsuarioCreado(),
                s.getCreadoEn(),
                s.getActualizadoEn()
        );
    }
}
