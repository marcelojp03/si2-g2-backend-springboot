package com.uagrm.si2g2.notificacion.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NotificacionRepository extends JpaRepository<Notificacion, UUID> {

    List<Notificacion> findByIdUsuarioAndLeidaFalseOrderByCreadoEnDesc(UUID idUsuario);

    List<Notificacion> findByIdUsuarioOrderByCreadoEnDesc(UUID idUsuario, Pageable pageable);

    long countByIdUsuarioAndLeidaFalse(UUID idUsuario);

    @Modifying
    @Query("UPDATE Notificacion n SET n.leida = true, n.leidaEn = CURRENT_TIMESTAMP WHERE n.idUsuario = :idUsuario AND n.leida = false")
    int marcarTodasLeidas(@Param("idUsuario") UUID idUsuario);
}
