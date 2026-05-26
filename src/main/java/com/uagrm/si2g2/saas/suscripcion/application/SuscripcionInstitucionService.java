package com.uagrm.si2g2.saas.suscripcion.application;

import com.uagrm.si2g2.auditoria.application.AuditoriaService;
import com.uagrm.si2g2.auth.domain.UsuarioRepository;
import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.saas.plan.domain.PlanSuscripcion;
import com.uagrm.si2g2.saas.plan.domain.PlanSuscripcionRepository;
import com.uagrm.si2g2.saas.suscripcion.domain.SuscripcionInstitucion;
import com.uagrm.si2g2.saas.suscripcion.domain.SuscripcionInstitucionRepository;
import com.uagrm.si2g2.saas.suscripcion.dto.SuscripcionInstitucionRequest;
import com.uagrm.si2g2.saas.suscripcion.dto.SuscripcionInstitucionResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SuscripcionInstitucionService {

    private final SuscripcionInstitucionRepository suscripcionRepo;
    private final PlanSuscripcionRepository planRepo;
    private final UsuarioRepository usuarioRepo;
    private final AuditoriaService auditoriaService;

    @Transactional
    public SuscripcionInstitucionResponse suscribir(SuscripcionInstitucionRequest req) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();

        if (suscripcionRepo.existsByIdInstitucionAndEstado(idInstitucion, "ACTIVA")) {
            throw new IllegalStateException(
                    "La institución ya tiene una suscripción activa. Cancélela antes de contratar un nuevo plan.");
        }

        PlanSuscripcion plan = planRepo.findByIdWithModulos(req.idPlan())
                .orElseThrow(() -> new EntityNotFoundException("Plan no encontrado: " + req.idPlan()));

        if (!"ACTIVO".equals(plan.getEstado())) {
            throw new IllegalStateException("El plan seleccionado no está disponible.");
        }

        // Validar que los usuarios actuales no superen el límite del nuevo plan
        long usuariosActuales = usuarioRepo.countByIdInstitucionAndEstado(idInstitucion, "ACTIVO");
        if (usuariosActuales > plan.getMaxUsuarios()) {
            throw new IllegalStateException(
                    "La institución tiene " + usuariosActuales + " usuarios activos, que supera el límite del plan ("
                            + plan.getMaxUsuarios() + "). Reduzca el número de usuarios antes de cambiar de plan.");
        }

        SuscripcionInstitucion suscripcion = SuscripcionInstitucion.builder()
                .idInstitucion(idInstitucion)
                .plan(plan)
                .fechaInicio(req.fechaInicio() != null ? req.fechaInicio() : LocalDate.now())
                .fechaFin(req.fechaFin())
                .observacion(req.observacion())
                .simulada(true)
                .build();

        suscripcionRepo.save(suscripcion);
        log.info("Institución {} suscrita al plan {}", idInstitucion, plan.getCodigo());
        auditoriaService.registrar(idInstitucion, SecurityUtils.currentUserId(),
                "SAAS", "SUSCRIPCION_CREAR", "suscripcion_institucion",
                suscripcion.getId().toString(), true,
                "Plan: " + plan.getCodigo());

        return SuscripcionInstitucionResponse.from(suscripcion);
    }

    @Transactional(readOnly = true)
    public SuscripcionInstitucionResponse obtenerActiva(UUID idInstitucion) {
        return suscripcionRepo.findActivaByIdInstitucion(idInstitucion)
                .map(SuscripcionInstitucionResponse::from)
                .orElseThrow(() -> new EntityNotFoundException(
                        "La institución no tiene suscripción activa"));
    }

    @Transactional
    public void cancelar(UUID idInstitucion) {
        SuscripcionInstitucion suscripcion = suscripcionRepo.findActivaByIdInstitucion(idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("No existe suscripción activa para cancelar"));
        suscripcion.setEstado("CANCELADA");
        suscripcionRepo.save(suscripcion);
        log.info("Suscripción cancelada para institución {}", idInstitucion);
        auditoriaService.registrar(idInstitucion, SecurityUtils.currentUserId(),
                "SAAS", "SUSCRIPCION_CANCELAR", "suscripcion_institucion",
                suscripcion.getId().toString(), true, null);
    }

    /**
     * Verifica si el número de usuarios activos de la institución respetaría el límite del plan vigente.
     * Lanza excepción si se superaría al agregar un usuario nuevo.
     */
    public void validarLimiteUsuarios(UUID idInstitucion) {
        suscripcionRepo.findActivaByIdInstitucion(idInstitucion).ifPresent(s -> {
            long actuales = usuarioRepo.countByIdInstitucionAndEstado(idInstitucion, "ACTIVO");
            if (actuales >= s.getPlan().getMaxUsuarios()) {
                throw new IllegalStateException(
                        "Se ha alcanzado el límite de usuarios del plan (" + s.getPlan().getMaxUsuarios()
                                + "). Para agregar más usuarios, actualice su plan.");
            }
        });
    }
}
