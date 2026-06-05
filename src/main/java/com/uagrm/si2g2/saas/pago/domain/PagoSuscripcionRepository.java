package com.uagrm.si2g2.saas.pago.domain;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface PagoSuscripcionRepository extends JpaRepository<PagoSuscripcion, UUID> {

    /** Último pago registrado para una solicitud (puede haber reintentos). */
    Optional<PagoSuscripcion> findFirstByIdSolicitudOrderByCreadoEnDesc(UUID idSolicitud);

    /** Busca un pago por su token público (link de pago). */
    Optional<PagoSuscripcion> findByTokenPago(UUID tokenPago);

    /** Bloquea el pago público para evitar carreras durante el polling. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PagoSuscripcion p where p.tokenPago = :tokenPago")
    Optional<PagoSuscripcion> findByTokenPagoForUpdate(UUID tokenPago);
}
