package com.uagrm.si2g2.calificacion.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PeriodoTrimestralRepository extends JpaRepository<PeriodoTrimestral, UUID> {

    Optional<PeriodoTrimestral> findByIdAndIdInstitucion(UUID id, UUID idInstitucion);

    Optional<PeriodoTrimestral> findByIdInstitucionAndIdGestionAcademicaAndNumeroTrimestre(
            UUID idInstitucion, UUID idGestionAcademica, Integer numeroTrimestre);

    List<PeriodoTrimestral> findAllByIdInstitucionAndIdGestionAcademica(UUID idInstitucion, UUID idGestionAcademica);
}