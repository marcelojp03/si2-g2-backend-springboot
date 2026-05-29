package com.uagrm.si2g2.calificacion.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActividadEvaluativaRepository extends JpaRepository<ActividadEvaluativa, UUID> {

    Optional<ActividadEvaluativa> findByIdAndIdInstitucion(UUID id, UUID idInstitucion);

    List<ActividadEvaluativa> findAllByIdInstitucionAndIdPeriodoTrimestral(UUID idInstitucion,
            UUID idPeriodoTrimestral);

    List<ActividadEvaluativa> findAllByIdInstitucionAndIdPeriodoTrimestralAndIdMateriaAndIdCursoAndIdParalelo(
            UUID idInstitucion, UUID idPeriodoTrimestral, UUID idMateria, UUID idCurso, UUID idParalelo);

    boolean existsByIdInstitucionAndIdPeriodoTrimestralAndNombreActividadIgnoreCase(
            UUID idInstitucion, UUID idPeriodoTrimestral, String nombreActividad);
}