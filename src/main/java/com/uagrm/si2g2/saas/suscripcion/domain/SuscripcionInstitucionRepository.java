package com.uagrm.si2g2.saas.suscripcion.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface SuscripcionInstitucionRepository extends JpaRepository<SuscripcionInstitucion, UUID> {

    @Query("SELECT s FROM SuscripcionInstitucion s JOIN FETCH s.plan p LEFT JOIN FETCH p.modulos WHERE s.idInstitucion = :idInstitucion AND s.estado = 'ACTIVA'")
    Optional<SuscripcionInstitucion> findActivaByIdInstitucion(UUID idInstitucion);

    boolean existsByIdInstitucionAndEstado(UUID idInstitucion, String estado);
}
