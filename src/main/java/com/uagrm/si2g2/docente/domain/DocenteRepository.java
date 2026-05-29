package com.uagrm.si2g2.docente.domain;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocenteRepository extends JpaRepository<Docente, UUID> {

    @EntityGraph(attributePaths = "materias")
    List<Docente> findAllByIdInstitucion(UUID idInstitucion);

    @EntityGraph(attributePaths = "materias")
    Optional<Docente> findByIdAndIdInstitucion(UUID id, UUID idInstitucion);

    Optional<Docente> findByIdUsuarioAndIdInstitucion(UUID idUsuario, UUID idInstitucion);

    boolean existsByIdInstitucionAndCodigo(UUID idInstitucion, String codigo);

    boolean existsByIdInstitucionAndDocumentoIdentidad(UUID idInstitucion, String documentoIdentidad);

    long countByIdInstitucion(UUID idInstitucion);

    long countByIdInstitucionAndEstado(UUID idInstitucion, String estado);
}
