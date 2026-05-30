package com.uagrm.si2g2.saas.plan.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanSuscripcionRepository extends JpaRepository<PlanSuscripcion, UUID> {

    Optional<PlanSuscripcion> findByCodigo(String codigo);

    List<PlanSuscripcion> findAllByEstado(String estado);

    boolean existsByCodigo(String codigo);

    @Query("SELECT p FROM PlanSuscripcion p LEFT JOIN FETCH p.modulos WHERE p.id = :id")
    Optional<PlanSuscripcion> findByIdWithModulos(UUID id);
}
