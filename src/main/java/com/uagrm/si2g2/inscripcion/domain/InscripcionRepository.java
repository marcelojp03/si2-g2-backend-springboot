package com.uagrm.si2g2.inscripcion.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InscripcionRepository extends JpaRepository<Inscripcion, UUID> {

    List<Inscripcion> findAllByIdInstitucion(UUID idInstitucion);

    List<Inscripcion> findAllByIdInstitucionAndIdEstudiante(UUID idInstitucion, UUID idEstudiante);

    List<Inscripcion> findAllByIdInstitucionAndIdGestion(UUID idInstitucion, UUID idGestion);

    List<Inscripcion> findAllByIdInstitucionAndIdParalelo(UUID idInstitucion, UUID idParalelo);

    Optional<Inscripcion> findByIdAndIdInstitucion(UUID id, UUID idInstitucion);

    boolean existsByIdInstitucionAndIdEstudianteAndIdGestionAndEstado(
            UUID idInstitucion, UUID idEstudiante, UUID idGestion, String estado);
}
