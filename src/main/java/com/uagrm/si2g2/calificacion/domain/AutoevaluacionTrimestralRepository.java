package com.uagrm.si2g2.calificacion.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AutoevaluacionTrimestralRepository extends JpaRepository<AutoevaluacionTrimestral, UUID> {

    Optional<AutoevaluacionTrimestral> findByIdInstitucionAndIdPeriodoEvaluacionAndIdMateriaAndIdEstudiante(
            UUID idInstitucion, UUID idPeriodoEvaluacion, UUID idMateria, UUID idEstudiante);

    List<AutoevaluacionTrimestral> findAllByIdInstitucionAndIdPeriodoEvaluacionAndIdMateria(
            UUID idInstitucion, UUID idPeriodoEvaluacion, UUID idMateria);

    Optional<AutoevaluacionTrimestral> findByIdPeriodoEvaluacionAndIdEstudianteAndIdMateria(
            UUID idPeriodoEvaluacion, UUID idEstudiante, UUID idMateria);
}