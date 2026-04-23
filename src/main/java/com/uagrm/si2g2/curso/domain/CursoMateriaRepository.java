package com.uagrm.si2g2.curso.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CursoMateriaRepository extends JpaRepository<CursoMateria, UUID> {

    List<CursoMateria> findAllByIdInstitucionAndIdCurso(UUID idInstitucion, UUID idCurso);

    List<CursoMateria> findAllByIdInstitucionAndIdMateria(UUID idInstitucion, UUID idMateria);

    Optional<CursoMateria> findByIdInstitucionAndIdCursoAndIdMateria(UUID idInstitucion, UUID idCurso, UUID idMateria);

    boolean existsByIdInstitucionAndIdCursoAndIdMateria(UUID idInstitucion, UUID idCurso, UUID idMateria);
}
