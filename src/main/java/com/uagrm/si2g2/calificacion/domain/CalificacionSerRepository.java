package com.uagrm.si2g2.calificacion.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CalificacionSerRepository extends JpaRepository<CalificacionSer, UUID> {

    Optional<CalificacionSer> findByIdInstitucionAndIdPeriodoEvaluacionAndIdMateriaAndIdEstudiante(
            UUID idInstitucion, UUID idPeriodoEvaluacion, UUID idMateria, UUID idEstudiante);

    List<CalificacionSer> findAllByIdInstitucionAndIdPeriodoEvaluacionAndIdMateria(
            UUID idInstitucion, UUID idPeriodoEvaluacion, UUID idMateria);

    Optional<CalificacionSer> findByIdPeriodoEvaluacionAndIdEstudianteAndIdMateria(
            UUID idPeriodoEvaluacion, UUID idEstudiante, UUID idMateria);
}