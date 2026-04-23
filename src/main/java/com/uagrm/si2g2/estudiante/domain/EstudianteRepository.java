package com.uagrm.si2g2.estudiante.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EstudianteRepository extends JpaRepository<Estudiante, UUID> {

    List<Estudiante> findAllByIdInstitucion(UUID idInstitucion);

    Optional<Estudiante> findByIdAndIdInstitucion(UUID id, UUID idInstitucion);

    boolean existsByIdInstitucionAndCodigoEstudiante(UUID idInstitucion, String codigoEstudiante);

    boolean existsByIdInstitucionAndDocumentoIdentidad(UUID idInstitucion, String documentoIdentidad);
}
