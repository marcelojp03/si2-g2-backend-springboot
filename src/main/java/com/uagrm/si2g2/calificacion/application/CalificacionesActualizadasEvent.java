package com.uagrm.si2g2.calificacion.application;

import java.util.Set;
import java.util.UUID;

public record CalificacionesActualizadasEvent(
        UUID idInstitucion,
        UUID idGestion,
        Set<UUID> idsEstudiante) {
}
