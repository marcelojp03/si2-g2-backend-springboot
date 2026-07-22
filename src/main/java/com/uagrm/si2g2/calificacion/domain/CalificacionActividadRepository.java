package com.uagrm.si2g2.calificacion.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface CalificacionActividadRepository extends JpaRepository<CalificacionActividad, UUID> {

    List<CalificacionActividad> findAllByIdActividad(UUID idActividad);

    Optional<CalificacionActividad> findByIdActividadAndIdEstudiante(UUID idActividad, UUID idEstudiante);

    boolean existsByIdActividad(UUID idActividad);

    List<CalificacionActividad> findAllByIdActividadAndIdEstudianteIn(UUID idActividad, List<UUID> idEstudiantes);

    List<CalificacionActividad> findAllByIdEstudianteAndIdActividadIn(
            UUID idEstudiante, Collection<UUID> idsActividad);

    List<CalificacionActividad> findAllByIdInstitucionAndIdActividadInAndIdEstudianteIn(
            UUID idInstitucion, Collection<UUID> idsActividad, Collection<UUID> idsEstudiante);

    Optional<CalificacionActividad> findByIdInstitucionAndIdActividadAndIdEstudiante(
            UUID idInstitucion, UUID idActividad, UUID idEstudiante);
}
