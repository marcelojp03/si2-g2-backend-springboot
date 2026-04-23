package com.uagrm.si2g2.docente.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocenteRepository extends JpaRepository<Docente, UUID> {

    List<Docente> findAllByIdInstitucion(UUID idInstitucion);

    Optional<Docente> findByIdAndIdInstitucion(UUID id, UUID idInstitucion);

    boolean existsByIdInstitucionAndCodigo(UUID idInstitucion, String codigo);

    boolean existsByIdInstitucionAndDocumentoIdentidad(UUID idInstitucion, String documentoIdentidad);
}
