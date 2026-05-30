package com.uagrm.si2g2.saas.solicitud.application;

import com.uagrm.si2g2.auditoria.application.AuditoriaService;
import com.uagrm.si2g2.auth.domain.*;
import com.uagrm.si2g2.institucion.domain.Institucion;
import com.uagrm.si2g2.institucion.domain.InstitucionRepository;
import com.uagrm.si2g2.saas.plan.domain.PlanSuscripcion;
import com.uagrm.si2g2.saas.plan.domain.PlanSuscripcionRepository;
import com.uagrm.si2g2.saas.solicitud.domain.SolicitudOnboarding;
import com.uagrm.si2g2.saas.solicitud.domain.SolicitudOnboardingRepository;
import com.uagrm.si2g2.saas.solicitud.dto.SolicitudAdminRequest;
import com.uagrm.si2g2.saas.solicitud.dto.SolicitudOnboardingRequest;
import com.uagrm.si2g2.saas.solicitud.dto.SolicitudOnboardingResponse;
import com.uagrm.si2g2.saas.suscripcion.domain.SuscripcionInstitucion;
import com.uagrm.si2g2.saas.suscripcion.domain.SuscripcionInstitucionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SolicitudOnboardingService {

    private final SolicitudOnboardingRepository solicitudRepo;
    private final PlanSuscripcionRepository planRepo;
    private final InstitucionRepository institucionRepo;
    private final UsuarioRepository usuarioRepo;
    private final RolRepository rolRepo;
    private final SuscripcionInstitucionRepository suscripcionRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;

    // ──────────────────────────────────────────────────────────────────────────────
    // PÚBLICA — sin autenticación
    // ──────────────────────────────────────────────────────────────────────────────

    @Transactional
    public SolicitudOnboardingResponse enviar(SolicitudOnboardingRequest req) {
        if (usuarioRepo.existsByCorreo(req.correoContacto())) {
            throw new IllegalStateException(
                    "El correo " + req.correoContacto() + " ya está registrado en el sistema.");
        }

        // Permitir reenviar si la anterior fue RECHAZADA; bloquear duplicados activos
        solicitudRepo.findByCorreoContactoAndEstadoNot(req.correoContacto(), "RECHAZADA")
                .ifPresent(s -> {
                    throw new IllegalStateException(
                            "Ya existe una solicitud en proceso para el correo " + req.correoContacto()
                                    + " (estado: " + s.getEstado() + ").");
                });

        PlanSuscripcion plan = planRepo.findById(req.idPlan())
                .orElseThrow(() -> new EntityNotFoundException("Plan no encontrado: " + req.idPlan()));

        if (!"ACTIVO".equals(plan.getEstado())) {
            throw new IllegalStateException("El plan seleccionado no está disponible.");
        }

        SolicitudOnboarding solicitud = SolicitudOnboarding.builder()
                .nombreInstitucion(req.nombreInstitucion())
                .tipoInstitucion(req.tipoInstitucion())
                .telefonoInstitucion(req.telefonoInstitucion())
                .correoInstitucion(req.correoInstitucion())
                .direccionInstitucion(req.direccionInstitucion())
                .nombresContacto(req.nombresContacto())
                .apellidosContacto(req.apellidosContacto())
                .correoContacto(req.correoContacto())
                .telefonoContacto(req.telefonoContacto())
                .plan(plan)
                .mensaje(req.mensaje())
                .estado("PENDIENTE_REVISION")
                .build();

        SolicitudOnboarding guardada = solicitudRepo.save(solicitud);
        log.info("Nueva solicitud onboarding: correo={}, institucion={}", req.correoContacto(), req.nombreInstitucion());
        return SolicitudOnboardingResponse.from(guardada);
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // SUPER_ADMIN
    // ──────────────────────────────────────────────────────────────────────────────

    public List<SolicitudOnboardingResponse> listar(String estado) {
        List<SolicitudOnboarding> lista = (estado != null && !estado.isBlank())
                ? solicitudRepo.findByEstadoWithPlan(estado)
                : solicitudRepo.findAllWithPlan();
        return lista.stream().map(SolicitudOnboardingResponse::from).toList();
    }

    public SolicitudOnboardingResponse obtener(UUID id) {
        return SolicitudOnboardingResponse.from(solicitudRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada: " + id)));
    }

    @Transactional
    public SolicitudOnboardingResponse aprobar(UUID id, SolicitudAdminRequest req) {
        SolicitudOnboarding s = requireEstado(id, "PENDIENTE_REVISION");
        s.setEstado("APROBADA");
        s.setNotasAdmin(req.notasAdmin());
        log.info("Solicitud aprobada: id={}", id);
        return SolicitudOnboardingResponse.from(solicitudRepo.save(s));
    }

    @Transactional
    public SolicitudOnboardingResponse rechazar(UUID id, SolicitudAdminRequest req) {
        SolicitudOnboarding s = requireEstado(id, "PENDIENTE_REVISION", "APROBADA");
        s.setEstado("RECHAZADA");
        s.setNotasAdmin(req.notasAdmin());
        log.info("Solicitud rechazada: id={}", id);
        return SolicitudOnboardingResponse.from(solicitudRepo.save(s));
    }

    @Transactional
    public SolicitudOnboardingResponse confirmarPago(UUID id) {
        SolicitudOnboarding s = requireEstado(id, "APROBADA");
        s.setEstado("PAGADO");
        log.info("Pago confirmado (simulado): id={}", id);
        return SolicitudOnboardingResponse.from(solicitudRepo.save(s));
    }

    @Transactional
    public SolicitudOnboardingResponse activar(UUID id) {
        SolicitudOnboarding s = requireEstado(id, "PAGADO");

        // 1. Crear institución
        String codigo = generarCodigoInstitucion(s.getNombreInstitucion());
        Institucion institucion = Institucion.builder()
                .codigo(codigo)
                .nombre(s.getNombreInstitucion())
                .tipoInstitucion(s.getTipoInstitucion())
                .telefono(s.getTelefonoInstitucion())
                .correo(s.getCorreoInstitucion())
                .direccion(s.getDireccionInstitucion())
                .estado("ACTIVO")
                .build();
        institucion = institucionRepo.save(institucion);
        final UUID idInstitucion = institucion.getId();

        // 2. Crear usuario ADMIN_INSTITUCION con contraseña temporal
        Rol rolAdmin = rolRepo.findByCodigo("ADMIN_INSTITUCION")
                .orElseThrow(() -> new EntityNotFoundException("Rol ADMIN_INSTITUCION no encontrado"));

        String contrasenaTemp = "Cambiar@" + (int)(Math.random() * 9000 + 1000);
        Usuario usuario = Usuario.builder()
                .idInstitucion(idInstitucion)
                .correo(s.getCorreoContacto())
                .hashContrasena(passwordEncoder.encode(contrasenaTemp))
                .nombres(s.getNombresContacto())
                .apellidos(s.getApellidosContacto())
                .telefono(s.getTelefonoContacto())
                .roles(Set.of(rolAdmin))
                .requiereCambioContrasena(true)
                .build();
        usuario = usuarioRepo.save(usuario);

        // 3. Crear suscripción activa
        SuscripcionInstitucion suscripcion = SuscripcionInstitucion.builder()
                .idInstitucion(idInstitucion)
                .plan(s.getPlan())
                .fechaInicio(LocalDate.now())
                .estado("ACTIVA")
                .simulada(true)
                .observacion("Activada desde solicitud onboarding #" + s.getId())
                .build();
        suscripcionRepo.save(suscripcion);

        // 4. Actualizar solicitud
        s.setEstado("ACTIVA");
        s.setIdInstitucionCreada(idInstitucion);
        s.setIdUsuarioCreado(usuario.getId());
        solicitudRepo.save(s);

        // 5. Auditoría
        auditoriaService.registrar(idInstitucion, usuario.getId(),
                "SAAS", "ACTIVAR_SOLICITUD", "solicitud_onboarding", s.getId().toString(),
                true, "Institución " + institucion.getNombre() + " activada. Plan: " + s.getPlan().getCodigo());

        log.info("Solicitud activada: institucion={}, usuario={}", idInstitucion, usuario.getId());
        return SolicitudOnboardingResponse.from(s);
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────────

    private SolicitudOnboarding requireEstado(UUID id, String... estadosPermitidos) {
        SolicitudOnboarding s = solicitudRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada: " + id));
        for (String est : estadosPermitidos) {
            if (est.equals(s.getEstado())) return s;
        }
        throw new IllegalStateException(
                "La solicitud está en estado '" + s.getEstado() + "' y no permite esta operación.");
    }

    private String generarCodigoInstitucion(String nombre) {
        String base = nombre.toUpperCase()
                .replaceAll("[^A-Z0-9]", "")
                .substring(0, Math.min(8, nombre.replaceAll("[^A-Za-z0-9]", "").length()));
        String codigo = base + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        // Garantizar unicidad
        int intento = 0;
        while (institucionRepo.findByCodigo(codigo).isPresent() && intento < 10) {
            codigo = base + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
            intento++;
        }
        return codigo;
    }
}
