package com.uagrm.si2g2.institucion.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConfiguracionInstitucionRepository extends JpaRepository<ConfiguracionInstitucion, UUID> {

    List<ConfiguracionInstitucion> findAllByIdInstitucion(UUID idInstitucion);

    Optional<ConfiguracionInstitucion> findByIdInstitucionAndClave(UUID idInstitucion, String clave);

    boolean existsByIdInstitucionAndClave(UUID idInstitucion, String clave);
}
