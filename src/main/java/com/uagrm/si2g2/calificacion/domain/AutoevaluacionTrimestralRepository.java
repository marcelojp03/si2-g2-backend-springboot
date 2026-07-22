package com.uagrm.si2g2.calificacion.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface AutoevaluacionTrimestralRepository extends JpaRepository<AutoevaluacionTrimestral, UUID> {

    Optional<AutoevaluacionTrimestral> findByIdInstitucionAndIdPeriodoEvaluacionAndIdMateriaAndIdEstudiante(
            UUID idInstitucion, UUID idPeriodoEvaluacion, UUID idMateria, UUID idEstudiante);

    List<AutoevaluacionTrimestral> findAllByIdInstitucionAndIdPeriodoEvaluacionAndIdMateria(
            UUID idInstitucion, UUID idPeriodoEvaluacion, UUID idMateria);

    List<AutoevaluacionTrimestral> findAllByIdInstitucionAndIdPeriodoEvaluacionAndIdMateriaAndIdEstudianteIn(
            UUID idInstitucion, UUID idPeriodoEvaluacion, UUID idMateria, Collection<UUID> idsEstudiante);

    List<AutoevaluacionTrimestral> findAllByIdInstitucionAndIdEstudianteAndIdPeriodoEvaluacionIn(
            UUID idInstitucion, UUID idEstudiante, Collection<UUID> idsPeriodoEvaluacion);

    Optional<AutoevaluacionTrimestral> findByIdPeriodoEvaluacionAndIdEstudianteAndIdMateria(
            UUID idPeriodoEvaluacion, UUID idEstudiante, UUID idMateria);
}
