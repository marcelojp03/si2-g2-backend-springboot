package com.uagrm.si2g2.saas.plan.application;

import com.uagrm.si2g2.auditoria.application.AuditoriaService;
import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.saas.plan.domain.ModuloSistema;
import com.uagrm.si2g2.saas.plan.domain.ModuloSistemaRepository;
import com.uagrm.si2g2.saas.plan.domain.PlanSuscripcion;
import com.uagrm.si2g2.saas.plan.domain.PlanSuscripcionRepository;
import com.uagrm.si2g2.saas.plan.dto.PlanSuscripcionRequest;
import com.uagrm.si2g2.saas.plan.dto.PlanSuscripcionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanSuscripcionService {

    private final PlanSuscripcionRepository planRepo;
    private final ModuloSistemaRepository moduloRepo;
    private final AuditoriaService auditoriaService;

    @Transactional
    public PlanSuscripcionResponse crear(PlanSuscripcionRequest req) {
        if (planRepo.existsByCodigo(req.codigo())) {
            throw new IllegalStateException("Ya existe un plan con el código: " + req.codigo());
        }
        Set<ModuloSistema> modulos = resolveModulos(req.idModulos());
        PlanSuscripcion plan = PlanSuscripcion.builder()
                .codigo(req.codigo().toUpperCase())
                .nombre(req.nombre())
                .descripcion(req.descripcion())
                .maxUsuarios(req.maxUsuarios())
                .maxAlmacenamientoMb(req.maxAlmacenamientoMb())
                .precioMensual(req.precioMensual())
                .modulos(modulos)
                .build();
        planRepo.save(plan);
        log.info("Plan creado: {}", plan.getCodigo());
        auditoriaService.registrar(null, SecurityUtils.currentUserId(),
                "SAAS", "PLAN_CREAR", "plan_suscripcion", plan.getId().toString(), true, null);
        return PlanSuscripcionResponse.from(plan);
    }

    @Transactional(readOnly = true)
    public List<PlanSuscripcionResponse> listar(String estado) {
        List<PlanSuscripcion> planes = (estado != null)
                ? planRepo.findAllByEstado(estado.toUpperCase())
                : planRepo.findAll();
        return planes.stream().map(PlanSuscripcionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PlanSuscripcionResponse obtener(UUID id) {
        return planRepo.findByIdWithModulos(id)
                .map(PlanSuscripcionResponse::from)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Plan no encontrado: " + id));
    }

    @Transactional
    public PlanSuscripcionResponse actualizar(UUID id, PlanSuscripcionRequest req) {
        PlanSuscripcion plan = planRepo.findByIdWithModulos(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Plan no encontrado: " + id));
        if (!plan.getCodigo().equalsIgnoreCase(req.codigo()) && planRepo.existsByCodigo(req.codigo())) {
            throw new IllegalStateException("Ya existe un plan con el código: " + req.codigo());
        }
        plan.setCodigo(req.codigo().toUpperCase());
        plan.setNombre(req.nombre());
        plan.setDescripcion(req.descripcion());
        plan.setMaxUsuarios(req.maxUsuarios());
        plan.setMaxAlmacenamientoMb(req.maxAlmacenamientoMb());
        plan.setPrecioMensual(req.precioMensual());
        plan.setModulos(resolveModulos(req.idModulos()));
        planRepo.save(plan);
        log.info("Plan actualizado: {}", plan.getCodigo());
        auditoriaService.registrar(null, SecurityUtils.currentUserId(),
                "SAAS", "PLAN_ACTUALIZAR", "plan_suscripcion", plan.getId().toString(), true, null);
        return PlanSuscripcionResponse.from(plan);
    }

    @Transactional
    public void desactivar(UUID id) {
        PlanSuscripcion plan = planRepo.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Plan no encontrado: " + id));
        plan.setEstado("INACTIVO");
        planRepo.save(plan);
        auditoriaService.registrar(null, SecurityUtils.currentUserId(),
                "SAAS", "PLAN_DESACTIVAR", "plan_suscripcion", id.toString(), true, null);
    }

    private Set<ModuloSistema> resolveModulos(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) return new HashSet<>();
        return new HashSet<>(moduloRepo.findAllById(ids));
    }
}
