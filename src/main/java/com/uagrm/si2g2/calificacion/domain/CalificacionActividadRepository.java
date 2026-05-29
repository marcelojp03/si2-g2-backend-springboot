package com.uagrm.si2g2.calificacion.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CalificacionActividadRepository extends JpaRepository<CalificacionActividad, UUID> {

    List<CalificacionActividad> findAllByIdActividad(UUID idActividad);

    Optional<CalificacionActividad> findByIdActividadAndIdEstudiante(UUID idActividad, UUID idEstudiante);
}