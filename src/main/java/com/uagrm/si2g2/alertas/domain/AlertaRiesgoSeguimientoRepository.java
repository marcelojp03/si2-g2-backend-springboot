package com.uagrm.si2g2.alertas.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AlertaRiesgoSeguimientoRepository extends JpaRepository<AlertaRiesgoSeguimiento, UUID> {
    List<AlertaRiesgoSeguimiento> findAllByIdAlertaRiesgoAndIdInstitucionOrderByCreadoEnAscIdAsc(
            UUID idAlertaRiesgo, UUID idInstitucion);
}
