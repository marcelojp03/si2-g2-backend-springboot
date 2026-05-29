package com.uagrm.si2g2.calificacion.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AutoevaluacionTrimestralRepository extends JpaRepository<AutoevaluacionTrimestral, UUID> {

    Optional<AutoevaluacionTrimestral> findByIdInstitucionAndIdGestionAcademicaAndIdTrimestreAndIdMateriaAndIdEstudiante(
            UUID idInstitucion, UUID idGestionAcademica, UUID idTrimestre, UUID idMateria, UUID idEstudiante);

    List<AutoevaluacionTrimestral> findAllByIdInstitucionAndIdGestionAcademicaAndIdTrimestreAndIdMateria(
            UUID idInstitucion, UUID idGestionAcademica, UUID idTrimestre, UUID idMateria);
}