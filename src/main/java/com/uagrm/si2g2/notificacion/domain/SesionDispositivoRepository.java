package com.uagrm.si2g2.notificacion.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SesionDispositivoRepository extends JpaRepository<SesionDispositivo, UUID> {

    Optional<SesionDispositivo> findByIdUsuarioAndTokenDispositivo(UUID idUsuario, String token);

    List<SesionDispositivo> findByIdUsuarioAndActivoTrue(UUID idUsuario);
}
