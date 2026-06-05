package com.uagrm.si2g2.saas.pago.application;

import com.uagrm.si2g2.auditoria.application.AuditoriaService;
import com.uagrm.si2g2.auth.application.AuthService;
import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.common.email.EmailService;
import com.uagrm.si2g2.config.AppProperties;
import com.uagrm.si2g2.saas.pago.domain.PagoSuscripcion;
import com.uagrm.si2g2.saas.pago.domain.PagoSuscripcionRepository;
import com.uagrm.si2g2.saas.pago.dto.EstadoPagoResponse;
import com.uagrm.si2g2.saas.pago.dto.PagoPublicoDto;
import com.uagrm.si2g2.saas.pago.dto.PagoSuscripcionResponse;
import com.uagrm.si2g2.saas.plan.domain.PlanSuscripcion;
import com.uagrm.si2g2.saas.solicitud.application.SolicitudOnboardingService;
import com.uagrm.si2g2.saas.solicitud.domain.SolicitudOnboarding;
import com.uagrm.si2g2.saas.solicitud.domain.SolicitudOnboardingRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Orquesta el pago del plan de una solicitud de onboarding contra la pasarela Vpay.
 *
 * <p>El pago es PRE-institución: se vincula a la solicitud, no a {@code id_institucion}.
 * Operación accesible sólo por SUPER_ADMIN, por lo que no se filtra por tenant.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PagoSuscripcionService {

    private final PagoSuscripcionRepository pagoRepo;
    private final SolicitudOnboardingRepository solicitudRepo;
    private final VpayClient vpayClient;
    private final EmailService emailService;
    private final AuditoriaService auditoriaService;
    private final SolicitudOnboardingService solicitudService;
    private final AuthService authService;
    private final AppProperties appProperties;

    /**
     * Genera (o reutiliza) el QR de pago para una solicitud aprobada y notifica al contacto.
     * Deja la solicitud en estado PENDIENTE_PAGO.
     */
    @Transactional
    public PagoSuscripcionResponse generarQrParaSolicitud(UUID idSolicitud) {
        SolicitudOnboarding solicitud = solicitudRepo.findById(idSolicitud)
                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada: " + idSolicitud));

        if (!"APROBADA".equals(solicitud.getEstado()) && !"PENDIENTE_PAGO".equals(solicitud.getEstado())) {
            throw new IllegalStateException(
                    "Sólo se puede generar el QR de pago para solicitudes APROBADA o PENDIENTE_PAGO (estado actual: "
                            + solicitud.getEstado() + ").");
        }

        // Reutiliza un QR pendiente vigente si existe (idempotencia).
        PagoSuscripcion existente = pagoRepo.findFirstByIdSolicitudOrderByCreadoEnDesc(idSolicitud).orElse(null);
        if (existente != null && "PENDIENTE".equals(existente.getEstado())
                && (existente.getFechaExpiracion() == null || !existente.getFechaExpiracion().isBefore(LocalDate.now()))
                && qrBase64Valida(existente.getQrBase64(), existente.getReferenciaExterna())) {
            log.info("[PAGO] Reutilizando QR pendiente para solicitud {}", idSolicitud);
            return PagoSuscripcionResponse.from(existente);
        }

        PlanSuscripcion plan = solicitud.getPlan();
        LocalDate expiracion = LocalDate.now().plusDays(7);
        String glosa = plan.getNombre() + " - " + solicitud.getNombreInstitucion();

        QrResult qr = vpayClient.generarQr(plan.getPrecioMensual(), glosa, expiracion, idSolicitud.toString());

        PagoSuscripcion pago = PagoSuscripcion.builder()
                .idSolicitud(idSolicitud)
                .idPlan(plan.getId())
                .monto(plan.getPrecioMensual())
                .moneda("BOB")
                .metodoPago("QR")
                .proveedor(vpayClient.isDemo() ? "VPAY_DEMO" : "VPAY")
                .referenciaExterna(qr.idQr())
                .qrBase64(qr.qrBase64())
                .estado("PENDIENTE")
                .glosa(glosa)
                .fechaExpiracion(expiracion)
                .build();
        pago = pagoRepo.save(pago);

        solicitud.setEstado("PENDIENTE_PAGO");
        solicitudRepo.save(solicitud);

        emailService.enviarQrPago(
                solicitud.getCorreoContacto(),
                solicitud.getNombresContacto(),
                plan.getNombre(),
                plan.getPrecioMensual(),
                "BOB",
                qr.qrBase64());

        auditoriaService.registrar(null, SecurityUtils.currentUserId(),
                "SAAS", "GENERAR_QR_PAGO", "pago_suscripcion", pago.getId().toString(),
                true, "QR generado para solicitud " + idSolicitud + " (" + glosa + ")");

        log.info("[PAGO] QR generado pago={} solicitud={}", pago.getId(), idSolicitud);
        return PagoSuscripcionResponse.from(pago);
    }

    /**
     * Consulta el estado del pago contra Vpay. Si está pagado, marca el pago y la solicitud
     * como pagados y envía la confirmación al contacto.
     */
    @Transactional
    public EstadoPagoResponse consultarEstado(UUID idPago) {
        PagoSuscripcion pago = pagoRepo.findById(idPago)
                .orElseThrow(() -> new EntityNotFoundException("Pago no encontrado: " + idPago));

        if ("PAGADO".equals(pago.getEstado())) {
            return new EstadoPagoResponse(pago.getId(), pago.getReferenciaExterna(), "PAG", true, pago.getEstado());
        }

        String estadoVpay = vpayClient.consultarEstadoQr(pago.getReferenciaExterna());
        if ("PAG".equals(estadoVpay)) {
            confirmarPagado(pago);
        }
        return new EstadoPagoResponse(pago.getId(), pago.getReferenciaExterna(),
                estadoVpay, "PAGADO".equals(pago.getEstado()), pago.getEstado());
    }

    /** Devuelve el pago vigente de una solicitud (o null si no tiene). */
    public PagoSuscripcionResponse obtenerPorSolicitud(UUID idSolicitud) {
        return pagoRepo.findFirstByIdSolicitudOrderByCreadoEnDesc(idSolicitud)
                .map(PagoSuscripcionResponse::from)
                .orElse(null);
    }

    // ── Endpoints públicos (sin autenticación) ─────────────────────────────────

    /**
     * Retorna el pago por token público y genera el QR lazily si aún no existe.
     * Usado por la página pública /pagar/{tokenPago}.
     */
    @Transactional
    public PagoPublicoDto obtenerYGenerarQrPublico(UUID tokenPago) {
        PagoSuscripcion pago = pagoRepo.findByTokenPago(tokenPago)
                .orElseThrow(() -> new EntityNotFoundException("Link de pago no válido o expirado"));

        if ("PAGADO".equals(pago.getEstado())) {
            return PagoPublicoDto.from(pago);
        }

        if (pago.getFechaExpiracion() != null && pago.getFechaExpiracion().isBefore(LocalDate.now())) {
            pago.setEstado("EXPIRADO");
            pagoRepo.save(pago);
            return PagoPublicoDto.from(pago);
        }

        // Generar QR lazily si no existe todavía
        if (!qrBase64Valida(pago.getQrBase64(), pago.getReferenciaExterna())) {
            SolicitudOnboarding solicitud = solicitudRepo.findById(pago.getIdSolicitud())
                    .orElseThrow(() -> new EntityNotFoundException("Solicitud asociada no encontrada"));

            QrResult qr = vpayClient.generarQr(pago.getMonto(), pago.getGlosa(),
                    pago.getFechaExpiracion(), pago.getIdSolicitud().toString());
            pago.setQrBase64(qr.qrBase64());
            pago.setReferenciaExterna(qr.idQr());
            pago.setProveedor(vpayClient.isDemo() ? "VPAY_DEMO" : "VPAY");
            pagoRepo.save(pago);

            // Avanzar solicitud a PENDIENTE_PAGO si aún estaba en APROBADA
            if ("APROBADA".equals(solicitud.getEstado())) {
                solicitud.setEstado("PENDIENTE_PAGO");
                solicitudRepo.save(solicitud);
            }
            log.info("[PAGO] QR generado lazily para tokenPago={}", tokenPago);
        }

        return PagoPublicoDto.from(pago);
    }

    /**
     * Valida que el contenido parezca una imagen QR en base64 y no un id textual.
     */
    private boolean qrBase64Valida(String qrBase64, String referenciaExterna) {
        if (qrBase64 == null || qrBase64.isBlank()) {
            return false;
        }
        if (referenciaExterna != null && qrBase64.equals(referenciaExterna)) {
            return false;
        }
        // Un PNG/JPEG base64 real de QR suele superar holgadamente este tamaño.
        return qrBase64.length() > 100;
    }

    /**
     * Consulta el estado del pago desde el endpoint público de polling.
     * Si Vpay confirma el pago (PAG), activa la institución automáticamente.
     */
    @Transactional
    public EstadoPagoResponse consultarEstadoPublico(UUID tokenPago) {
        PagoSuscripcion pago = pagoRepo.findByTokenPagoForUpdate(tokenPago)
                .orElseThrow(() -> new EntityNotFoundException("Link de pago no válido"));

        if ("PAGADO".equals(pago.getEstado())) {
            return new EstadoPagoResponse(pago.getId(), pago.getReferenciaExterna(), "PAG", true, pago.getEstado());
        }

        if (pago.getReferenciaExterna() == null) {
            // QR aún no generado — el cliente debe cargar la página primero
            return new EstadoPagoResponse(pago.getId(), null, "PEN", false, pago.getEstado());
        }

        String estadoVpay = vpayClient.consultarEstadoQr(pago.getReferenciaExterna());
        if ("PAG".equals(estadoVpay)) {
            confirmarPagadoYActivar(pago);
        }
        return new EstadoPagoResponse(pago.getId(), pago.getReferenciaExterna(),
                estadoVpay, "PAGADO".equals(pago.getEstado()), pago.getEstado());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private void confirmarPagado(PagoSuscripcion pago) {
        pago.setEstado("PAGADO");
        pago.setPagadoEn(Instant.now());
        pagoRepo.save(pago);

        SolicitudOnboarding solicitud = solicitudRepo.findById(pago.getIdSolicitud()).orElse(null);
        if (solicitud != null && !"ACTIVA".equals(solicitud.getEstado())) {
            solicitud.setEstado("PAGADO");
            solicitudRepo.save(solicitud);

            emailService.enviarConfirmacionPago(
                    solicitud.getCorreoContacto(),
                    solicitud.getNombresContacto(),
                    solicitud.getPlan().getNombre(),
                    pago.getMonto(),
                    pago.getMoneda());
        }

        auditoriaService.registrar(null, SecurityUtils.currentUserId(),
                "SAAS", "CONFIRMAR_PAGO", "pago_suscripcion", pago.getId().toString(),
                true, "Pago confirmado por Vpay para solicitud " + pago.getIdSolicitud());

        log.info("[PAGO] Pago confirmado pago={} solicitud={}", pago.getId(), pago.getIdSolicitud());
    }

    /**
     * Confirma el pago vía polling público y activa la institución automáticamente.
     * Genera un challenge de activación para que el nuevo admin cree su contraseña.
     */
    private void confirmarPagadoYActivar(PagoSuscripcion pago) {
        if ("PAGADO".equals(pago.getEstado())) {
            return;
        }

        pago.setEstado("PAGADO");
        pago.setPagadoEn(Instant.now());
        pagoRepo.save(pago);

        SolicitudOnboarding solicitud = solicitudRepo.findById(pago.getIdSolicitud()).orElse(null);
        if (solicitud == null || "ACTIVA".equals(solicitud.getEstado())) {
            return;
        }

        solicitud.setEstado("PAGADO");
        solicitudRepo.save(solicitud);

        auditoriaService.registrar(null, null,
                "SAAS", "CONFIRMAR_PAGO_PUBLICO", "pago_suscripcion", pago.getId().toString(),
                true, "Pago confirmado por Vpay (flujo público) para solicitud " + pago.getIdSolicitud());

        // Activar institución automáticamente
        try {
            SolicitudOnboardingService.ActivacionResult activacion =
                    solicitudService.activarDesde(solicitud.getId());

            // Generar link de primera contraseña (challenge pre-verificado)
            AuthService.ChallengeActivacion challenge =
                    authService.generarChallengeActivacion(activacion.idUsuario(), activacion.correo());

            String linkContrasena = appProperties.getFrontendUrl()
                    + "/auth/crear-contrasena?challengeId=" + challenge.challengeId()
                    + "&token=" + challenge.recoveryToken();

            emailService.enviarBienvenidaConActivacion(
                    activacion.correo(),
                    solicitud.getNombresContacto(),
                    activacion.nombrePlan(),
                    activacion.correo(),
                    linkContrasena);

            log.info("[PAGO] Institución activada automáticamente tras pago. solicitud={}",
                    solicitud.getId());
        } catch (Exception ex) {
            log.error("[PAGO] Error al activar institución tras pago confirmado (solicitud={}): {}",
                    solicitud.getId(), ex.getMessage(), ex);
        }
    }
}
