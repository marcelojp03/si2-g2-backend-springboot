package com.uagrm.si2g2.calificacion.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ObservacionSerRepository extends JpaRepository<ObservacionSer, UUID> {

    List<ObservacionSer> findAllByIdInstitucionAndIdPeriodoEvaluacionAndIdEstudianteAndIdMateria(
            UUID idInstitucion, UUID idPeriodoEvaluacion, UUID idEstudiante, UUID idMateria);

    List<ObservacionSer> findAllByIdInstitucionAndIdPeriodoEvaluacionAndIdDocenteAndFechaObservacionBetween(
            UUID idInstitucion, UUID idPeriodoEvaluacion, UUID idDocente,
            java.time.LocalDate fechaInicio, java.time.LocalDate fechaFin);

    boolean existsByIdPeriodoEvaluacionAndIdEstudianteAndIdMateria(
            UUID idPeriodoEvaluacion, UUID idEstudiante, UUID idMateria);
}