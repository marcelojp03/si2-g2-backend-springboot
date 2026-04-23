package com.uagrm.si2g2.tutor.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TutorRepository extends JpaRepository<Tutor, UUID> {

    List<Tutor> findAllByIdInstitucion(UUID idInstitucion);

    Optional<Tutor> findByIdAndIdInstitucion(UUID id, UUID idInstitucion);

    boolean existsByIdInstitucionAndDocumentoIdentidad(UUID idInstitucion, String documentoIdentidad);
}
