package com.uagrm.si2g2.saas.solicitud.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SolicitudOnboardingRepository extends JpaRepository<SolicitudOnboarding, UUID> {

    List<SolicitudOnboarding> findAllByOrderByCreadoEnDesc();

    List<SolicitudOnboarding> findByEstadoOrderByCreadoEnDesc(String estado);

    Optional<SolicitudOnboarding> findByCorreoContactoAndEstadoNot(String correo, String estado);

    @Query("SELECT s FROM SolicitudOnboarding s JOIN FETCH s.plan ORDER BY s.creadoEn DESC")
    List<SolicitudOnboarding> findAllWithPlan();

    @Query("SELECT s FROM SolicitudOnboarding s JOIN FETCH s.plan WHERE s.estado = :estado ORDER BY s.creadoEn DESC")
    List<SolicitudOnboarding> findByEstadoWithPlan(@Param("estado") String estado);
}
