package com.uagrm.si2g2.calificacion.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CalificacionActividadRepository extends JpaRepository<CalificacionActividad, UUID> {

    List<CalificacionActividad> findAllByIdActividad(UUID idActividad);

    Optional<CalificacionActividad> findByIdActividadAndIdEstudiante(UUID idActividad, UUID idEstudiante);

<<<<<<< HEAD
    boolean existsByIdActividad(UUID idActividad);
}
=======
    List<CalificacionActividad> findAllByIdActividadAndIdEstudianteIn(UUID idActividad, List<UUID> idEstudiantes);

    Optional<CalificacionActividad> findByIdInstitucionAndIdActividadAndIdEstudiante(
            UUID idInstitucion, UUID idActividad, UUID idEstudiante);
}
>>>>>>> d46e179 (feat: dimensiones dinámicas, validación de períodos, permisos granulares, seed sintético)
