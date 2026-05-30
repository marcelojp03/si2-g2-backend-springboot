package com.uagrm.si2g2.auth.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface IntentoLoginRepository extends JpaRepository<IntentoLogin, UUID> {

    /**
     * Cuenta intentos fallidos de un correo en los últimos N minutos (para detección de fuerza bruta).
     */
    @Query("SELECT COUNT(i) FROM IntentoLogin i WHERE i.correo = :correo AND i.exito = false AND i.fechaIntento >= :desde")
    long countFallidosDesde(@Param("correo") String correo, @Param("desde") Instant desde);

    /**
     * Búsqueda filtrada con paginación para el endpoint de consulta.
     */
    @Query("""
            SELECT i FROM IntentoLogin i
            WHERE (:idInstitucion IS NULL OR i.idInstitucion = :idInstitucion)
              AND (:correo IS NULL OR LOWER(i.correo) LIKE LOWER(CONCAT('%', :correo, '%')))
              AND (:soloFallos = false OR i.exito = false)
              AND i.fechaIntento >= :desde
              AND i.fechaIntento <= :hasta
            """)
    List<IntentoLogin> buscarConFiltros(
            @Param("idInstitucion") UUID idInstitucion,
            @Param("correo") String correo,
            @Param("soloFallos") boolean soloFallos,
            @Param("desde") Instant desde,
            @Param("hasta") Instant hasta,
            Pageable pageable);
}

