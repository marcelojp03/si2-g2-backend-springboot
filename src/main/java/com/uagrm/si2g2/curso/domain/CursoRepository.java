package com.uagrm.si2g2.curso.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CursoRepository extends JpaRepository<Curso, UUID> {

    List<Curso> findAllByIdInstitucion(UUID idInstitucion);

    Optional<Curso> findByIdAndIdInstitucion(UUID id, UUID idInstitucion);

    boolean existsByIdInstitucionAndNombre(UUID idInstitucion, String nombre);
}
