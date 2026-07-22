package com.uagrm.si2g2.calificacion.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface CalificacionSerRepository extends JpaRepository<CalificacionSer, UUID> {

    Optional<CalificacionSer> findByIdInstitucionAndIdPeriodoEvaluacionAndIdMateriaAndIdEstudiante(
            UUID idInstitucion, UUID idPeriodoEvaluacion, UUID idMateria, UUID idEstudiante);

    List<CalificacionSer> findAllByIdInstitucionAndIdPeriodoEvaluacionAndIdMateria(
            UUID idInstitucion, UUID idPeriodoEvaluacion, UUID idMateria);

    List<CalificacionSer> findAllByIdInstitucionAndIdPeriodoEvaluacionAndIdMateriaAndIdEstudianteIn(
            UUID idInstitucion, UUID idPeriodoEvaluacion, UUID idMateria, Collection<UUID> idsEstudiante);

    List<CalificacionSer> findAllByIdInstitucionAndIdEstudianteAndIdPeriodoEvaluacionIn(
            UUID idInstitucion, UUID idEstudiante, Collection<UUID> idsPeriodoEvaluacion);

    Optional<CalificacionSer> findByIdPeriodoEvaluacionAndIdEstudianteAndIdMateria(
            UUID idPeriodoEvaluacion, UUID idEstudiante, UUID idMateria);
}
