package com.uagrm.si2g2.pagos.application;

import com.uagrm.si2g2.auditoria.application.AuditoriaService;
import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.pagos.domain.PlanPago;
import com.uagrm.si2g2.pagos.domain.PlanPagoRepository;
import com.uagrm.si2g2.pagos.dto.PlanPagoRequest;
import com.uagrm.si2g2.pagos.dto.PlanPagoResponse;
import com.uagrm.si2g2.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlanPagoService {

    private final PlanPagoRepository repository;
    private final AuditoriaService auditoriaService;

    @Transactional
    public PlanPagoResponse crear(PlanPagoRequest request) {
        UUID idInstitucion = TenantContext.get();
        if (repository.existsByIdInstitucionAndNombreIgnoreCase(idInstitucion, request.getNombre())) {
            throw new IllegalStateException("Ya existe un plan con el nombre: " + request.getNombre());
        }
        PlanPago plan = PlanPago.builder()
                .idInstitucion(idInstitucion)
                .nombre(request.getNombre().trim())
                .tipoPeriodo(request.getTipoPeriodo())
                .monto(request.getMonto())
                .moneda(request.getMoneda() != null ? request.getMoneda() : "BOB")
                .cantidadCuotas(request.getCantidadCuotas())
                .diaVencimiento(request.getDiaVencimiento() != null ? request.getDiaVencimiento() : 10)
                .descripcion(request.getDescripcion())
                .build();
        PlanPagoResponse resp = PlanPagoResponse.from(repository.save(plan));
        auditoriaService.registrar(idInstitucion, SecurityUtils.currentUserId(),
                "PAGOS", "CREAR_PLAN", "plan_pago", resp.getId().toString(),
                true, "Plan de pago creado: " + resp.getNombre());
        return resp;
    }

    @Transactional(readOnly = true)
    public List<PlanPagoResponse> listar(boolean soloActivos) {
        UUID idInstitucion = TenantContext.get();
        if (soloActivos) {
            return repository.findByIdInstitucionAndActivoTrueOrderByNombre(idInstitucion)
                    .stream().map(PlanPagoResponse::from).toList();
        }
        return repository.findByIdInstitucionOrderByNombre(idInstitucion)
                .stream().map(PlanPagoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PlanPagoResponse obtener(UUID id) {
        return PlanPagoResponse.from(buscar(id));
    }

    @Transactional
    public PlanPagoResponse actualizar(UUID id, PlanPagoRequest request) {
        UUID idInstitucion = TenantContext.get();
        PlanPago plan = buscar(id);
        plan.setNombre(request.getNombre().trim());
        plan.setTipoPeriodo(request.getTipoPeriodo());
        plan.setMonto(request.getMonto());
        plan.setCantidadCuotas(request.getCantidadCuotas());
        plan.setDiaVencimiento(request.getDiaVencimiento() != null ? request.getDiaVencimiento() : 10);
        plan.setDescripcion(request.getDescripcion());
        if (request.getMoneda() != null) plan.setMoneda(request.getMoneda());
        PlanPagoResponse resp = PlanPagoResponse.from(repository.save(plan));
        auditoriaService.registrar(idInstitucion, SecurityUtils.currentUserId(),
                "PAGOS", "ACTUALIZAR_PLAN", "plan_pago", id.toString(),
                true, "Plan actualizado: " + resp.getNombre());
        return resp;
    }

    @Transactional
    public void desactivar(UUID id) {
        PlanPago plan = buscar(id);
        plan.setActivo(false);
        repository.save(plan);
        auditoriaService.registrar(TenantContext.get(), SecurityUtils.currentUserId(),
                "PAGOS", "DESACTIVAR_PLAN", "plan_pago", id.toString(),
                true, "Plan desactivado: " + plan.getNombre());
    }

    private PlanPago buscar(UUID id) {
        return repository.findByIdAndIdInstitucion(id, TenantContext.get())
                .orElseThrow(() -> new EntityNotFoundException("Plan de pago no encontrado: " + id));
    }
}
