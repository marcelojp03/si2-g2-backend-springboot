package com.uagrm.si2g2.asistencia.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AsistenciaRegistroRepository extends JpaRepository<AsistenciaRegistro, UUID> {

    Optional<AsistenciaRegistro> findByIdAndIdInstitucion(UUID id, UUID idInstitucion);

    Optional<AsistenciaRegistro> findByIdInstitucionAndIdAsignacionDocenteAndFecha(
            UUID idInstitucion,
            UUID idAsignacionDocente,
            LocalDate fecha
    );

    List<AsistenciaRegistro> findAllByIdInstitucionAndFecha(UUID idInstitucion, LocalDate fecha);

    List<AsistenciaRegistro> findAllByIdInstitucionAndIdAsignacionDocente(
            UUID idInstitucion,
            UUID idAsignacionDocente
    );

    boolean existsByIdInstitucionAndIdAsignacionDocenteAndFecha(
            UUID idInstitucion,
            UUID idAsignacionDocente,
            LocalDate fecha
    );
}