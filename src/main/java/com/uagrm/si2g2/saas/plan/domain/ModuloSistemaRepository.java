package com.uagrm.si2g2.saas.plan.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ModuloSistemaRepository extends JpaRepository<ModuloSistema, UUID> {

    Optional<ModuloSistema> findByCodigo(String codigo);

    List<ModuloSistema> findAllByEstadoOrderByOrdenVisualAsc(String estado);

    boolean existsByCodigo(String codigo);
}
