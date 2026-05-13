package com.uagrm.si2g2.auth.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RolRepository extends JpaRepository<Rol, UUID> {

    Optional<Rol> findByCodigo(String codigo);

    List<Rol> findAllByEsGlobalTrueAndEstadoOrderByNombreAsc(String estado);

    List<Rol> findAllByIdInstitucionAndEstadoOrderByNombreAsc(UUID idInstitucion, String estado);

    Optional<Rol> findByIdAndIdInstitucion(UUID id, UUID idInstitucion);

    boolean existsByIdInstitucionAndNombreIgnoreCase(UUID idInstitucion, String nombre);
}
