package com.uagrm.si2g2.curso.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParaleloRepository extends JpaRepository<Paralelo, UUID> {

    List<Paralelo> findAllByIdInstitucion(UUID idInstitucion);

    List<Paralelo> findAllByIdInstitucionAndIdCurso(UUID idInstitucion, UUID idCurso);

    List<Paralelo> findAllByIdInstitucionAndIdGestion(UUID idInstitucion, UUID idGestion);

    Optional<Paralelo> findByIdAndIdInstitucion(UUID id, UUID idInstitucion);

    boolean existsByIdInstitucionAndIdCursoAndNombreAndIdGestion(UUID idInstitucion, UUID idCurso, String nombre, UUID idGestion);
}
