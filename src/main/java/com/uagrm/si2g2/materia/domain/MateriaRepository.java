package com.uagrm.si2g2.materia.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MateriaRepository extends JpaRepository<Materia, UUID> {

    List<Materia> findAllByIdInstitucion(UUID idInstitucion);

    Optional<Materia> findByIdAndIdInstitucion(UUID id, UUID idInstitucion);

    boolean existsByIdInstitucionAndCodigo(UUID idInstitucion, String codigo);

    boolean existsByIdInstitucionAndNombre(UUID idInstitucion, String nombre);
}
