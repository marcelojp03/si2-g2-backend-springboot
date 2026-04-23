package com.uagrm.si2g2.institucion.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InstitucionRepository extends JpaRepository<Institucion, UUID> {

    boolean existsByCodigo(String codigo);

    Optional<Institucion> findByCodigo(String codigo);

    List<Institucion> findAllByEstado(String estado);
}
