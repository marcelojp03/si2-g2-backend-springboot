package com.uagrm.si2g2.calificacion.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActividadEvaluativaRepository extends JpaRepository<ActividadEvaluativa, UUID> {

    Optional<ActividadEvaluativa> findByIdAndIdInstitucion(UUID id, UUID idInstitucion);

    List<ActividadEvaluativa> findAllByIdInstitucionAndIdPeriodoEvaluacion(UUID idInstitucion, UUID idPeriodoEvaluacion);

    List<ActividadEvaluativa> findAllByIdInstitucionAndIdPeriodoEvaluacionAndDimension(
            UUID idInstitucion, UUID idPeriodoEvaluacion, String dimension);

    List<ActividadEvaluativa> findAllByIdInstitucionAndIdPeriodoEvaluacionAndIdMateria(
            UUID idInstitucion, UUID idPeriodoEvaluacion, UUID idMateria);

    List<ActividadEvaluativa> findAllByIdInstitucionAndIdPeriodoEvaluacionAndIdMateriaAndDimension(
            UUID idInstitucion, UUID idPeriodoEvaluacion, UUID idMateria, String dimension);

    boolean existsByIdInstitucionAndIdPeriodoEvaluacionAndNombreActividadIgnoreCase(
            UUID idInstitucion, UUID idPeriodoEvaluacion, String nombreActividad);
}