package com.uagrm.si2g2.pagos.application;

import com.uagrm.si2g2.auditoria.application.AuditoriaService;
import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.pagos.domain.CuotaEstudiante;
import com.uagrm.si2g2.pagos.domain.CuotaEstudianteRepository;
import com.uagrm.si2g2.pagos.domain.Pago;
import com.uagrm.si2g2.pagos.domain.PagoRepository;
import com.uagrm.si2g2.pagos.domain.PlanPago;
import com.uagrm.si2g2.pagos.domain.PlanPagoRepository;
import com.uagrm.si2g2.pagos.dto.CuotaEstudianteResponse;
import com.uagrm.si2g2.pagos.dto.PagoResponse;
import com.uagrm.si2g2.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CuotaService {

    private final CuotaEstudianteRepository cuotaRepository;
    private final PagoRepository pagoRepository;
    private final PlanPagoRepository planRepository;
    private final AuditoriaService auditoriaService;

    @Transactional
    public int generarCuotas(UUID idInstitucion, UUID idPlanPago, UUID idGestion, UUID idEstudiante) {
        PlanPago plan = planRepository.findByIdAndIdInstitucion(idPlanPago, idInstitucion)
                .orElseThrow(() -> new IllegalArgumentException("Plan no encontrado"));

        int creadas = 0;
        for (int i = 1; i <= plan.getCantidadCuotas(); i++) {
            if (cuotaRepository.existsByIdEstudianteAndIdPlanPagoAndNumeroCuota(idEstudiante, idPlanPago, i)) {
                continue;
            }
            LocalDate vencimiento = calcularVencimiento(plan, i);
            cuotaRepository.save(CuotaEstudiante.builder()
                    .idInstitucion(idInstitucion)
                    .idEstudiante(idEstudiante)
                    .idPlanPago(idPlanPago)
                    .idGestionAcademica(idGestion)
                    .numeroCuota(i)
                    .monto(plan.getMonto())
                    .fechaVencimiento(vencimiento)
                    .build());
            creadas++;
        }
        if (creadas > 0) {
            log.info("Generadas {} cuotas para estudiante {} plan {}", creadas, idEstudiante, idPlanPago);
        }
        return creadas;
    }

    @Transactional(readOnly = true)
    public List<CuotaEstudianteResponse> listarPorEstudiante(UUID idEstudiante, UUID idGestion) {
        if (idGestion != null) {
            return cuotaRepository.findByIdEstudianteAndIdGestionAcademicaOrderByNumeroCuota(idEstudiante, idGestion)
                    .stream().map(this::toResponse).toList();
        }
        return cuotaRepository.findByIdEstudianteOrderByNumeroCuota(idEstudiante)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CuotaEstudianteResponse> listarPorInstitucion(UUID idInstitucion) {
        return cuotaRepository.findByIdInstitucionAndEstado(idInstitucion, "PENDIENTE")
                .stream().map(this::toResponse).toList();
    }

    private CuotaEstudianteResponse toResponse(CuotaEstudiante c) {
        var pagos = pagoRepository.findByIdCuotaOrderByCreadoEnDesc(c.getId());
        var ultimoPago = pagos.isEmpty() ? null : PagoResponse.from(pagos.getFirst());
        String nombrePlan = "";
        try {
            nombrePlan = planRepository.findById(c.getIdPlanPago()).map(PlanPago::getNombre).orElse("");
        } catch (Exception e) {
            // ignore
        }
        return CuotaEstudianteResponse.builder()
                .id(c.getId())
                .idInstitucion(c.getIdInstitucion())
                .idEstudiante(c.getIdEstudiante())
                .idPlanPago(c.getIdPlanPago())
                .nombrePlan(nombrePlan)
                .idGestionAcademica(c.getIdGestionAcademica())
                .numeroCuota(c.getNumeroCuota())
                .monto(c.getMonto())
                .fechaVencimiento(c.getFechaVencimiento())
                .estado(c.getEstado())
                .creadoEn(c.getCreadoEn())
                .ultimoPago(ultimoPago)
                .build();
    }

    private LocalDate calcularVencimiento(PlanPago plan, int numeroCuota) {
        int dia = plan.getDiaVencimiento() != null ? plan.getDiaVencimiento() : 10;
        int mesOffset = switch (plan.getTipoPeriodo()) {
            case "MENSUAL" -> numeroCuota - 1;
            case "TRIMESTRAL" -> (numeroCuota - 1) * 3;
            case "SEMESTRAL" -> (numeroCuota - 1) * 6;
            case "ANUAL" -> (numeroCuota - 1) * 12;
            default -> numeroCuota - 1;
        };
        LocalDate base = LocalDate.now().withDayOfMonth(1).plusMonths(mesOffset);
        int maxDia = base.lengthOfMonth();
        return base.withDayOfMonth(Math.min(dia, maxDia));
    }
}
