package com.uagrm.si2g2.auth.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PermisoRepository extends JpaRepository<Permiso, UUID> {

    Optional<Permiso> findByCodigo(String codigo);

    List<Permiso> findAllByEstadoOrderByModuloAscAccionAsc(String estado);

    List<Permiso> findAllByIdIn(Collection<UUID> ids);
}
