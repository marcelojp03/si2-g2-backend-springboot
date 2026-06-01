package com.uagrm.si2g2.saas.pago.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PagoSuscripcionRepository extends JpaRepository<PagoSuscripcion, UUID> {

    /** Último pago registrado para una solicitud (puede haber reintentos). */
    Optional<PagoSuscripcion> findFirstByIdSolicitudOrderByCreadoEnDesc(UUID idSolicitud);

    /** Busca un pago por su token público (link de pago). */
    Optional<PagoSuscripcion> findByTokenPago(UUID tokenPago);
}
