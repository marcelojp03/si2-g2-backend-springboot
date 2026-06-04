package com.uagrm.si2g2.calificacion.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PeriodoEvaluacionRepository extends JpaRepository<PeriodoEvaluacion, UUID> {

    Optional<PeriodoEvaluacion> findByIdAndIdInstitucion(UUID id, UUID idInstitucion);

    Optional<PeriodoEvaluacion> findByIdInstitucionAndIdGestionAcademicaAndNumeroPeriodo(
            UUID idInstitucion, UUID idGestionAcademica, Integer numeroPeriodo);

    List<PeriodoEvaluacion> findAllByIdInstitucionAndIdGestionAcademica(UUID idInstitucion, UUID idGestionAcademica);

    List<PeriodoEvaluacion> findAllByIdInstitucionAndIdGestionAcademicaAndEstado(
            UUID idInstitucion, UUID idGestionAcademica, String estado);
}