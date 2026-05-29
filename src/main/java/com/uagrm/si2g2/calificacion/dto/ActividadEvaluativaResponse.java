package com.uagrm.si2g2.calificacion.dto;

import com.uagrm.si2g2.calificacion.domain.ActividadEvaluativa;

import java.time.Instant;
import java.util.UUID;

public record ActividadEvaluativaResponse(
        UUID id,
        UUID idPeriodoTrimestral,
        UUID idGestionAcademica,
        UUID idCurso,
        UUID idParalelo,
        UUID idMateria,
        UUID idDocente,
        String nombreActividad,
        String tipoActividad,
        String dimension,
        Integer puntajeMaximo,
        Instant fechaActividad,
        String descripcion,
        String estado,
        Instant creadoEn,
        Instant actualizadoEn) {

    public static ActividadEvaluativaResponse from(ActividadEvaluativa actividad) {
        return new ActividadEvaluativaResponse(
                actividad.getId(),
                actividad.getIdPeriodoTrimestral(),
                actividad.getIdGestionAcademica(),
                actividad.getIdCurso(),
                actividad.getIdParalelo(),
                actividad.getIdMateria(),
                actividad.getIdDocente(),
                actividad.getNombreActividad(),
                actividad.getTipoActividad(),
                actividad.getDimension(),
                actividad.getPuntajeMaximo(),
                actividad.getFechaActividad(),
                actividad.getDescripcion(),
                actividad.getEstado(),
                actividad.getCreadoEn(),
                actividad.getActualizadoEn());
    }
}