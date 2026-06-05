package com.uagrm.si2g2.dimension.application;

import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.dimension.domain.PeriodoDimensionRepository;
import com.uagrm.si2g2.dimension.domain.SolicitudEliminacionDimension;
import com.uagrm.si2g2.dimension.domain.SolicitudEliminacionDimensionRepository;
import com.uagrm.si2g2.dimension.dto.SolicitudEliminacionRequest;
import com.uagrm.si2g2.dimension.dto.SolicitudEliminacionResponse;
import com.uagrm.si2g2.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SolicitudEliminacionDimensionService {

    private final SolicitudEliminacionDimensionRepository solicitudRepository;
    private final PeriodoDimensionRepository periodoDimensionRepository;

    @Transactional
    public SolicitudEliminacionResponse crearSolicitud(UUID idPeriodoEvaluacion,
                                                        SolicitudEliminacionRequest request) {
        UUID idInstitucion = TenantContext.getOrThrow();
        UUID idUsuario = SecurityUtils.currentUserId();

        if (solicitudRepository.existsByIdPeriodoEvaluacionAndIdDimensionAndEstado(
                idPeriodoEvaluacion, request.idDimension(), "PENDIENTE")) {
            throw new IllegalArgumentException("Ya existe una solicitud pendiente para esta dimensión en este período");
        }

        SolicitudEliminacionDimension solicitud = SolicitudEliminacionDimension.builder()
                .idInstitucion(idInstitucion)
                .idPeriodoEvaluacion(idPeriodoEvaluacion)
                .idDimension(request.idDimension())
                .estado("PENDIENTE")
                .idUsuarioSolicitud(idUsuario)
                .fechaSolicitud(Instant.now())
                .observacion(request.observacion())
                .build();
        return SolicitudEliminacionResponse.from(solicitudRepository.save(solicitud));
    }

    public List<SolicitudEliminacionResponse> listarPendientes() {
        UUID idInstitucion = TenantContext.getOrThrow();
        return solicitudRepository
                .findAllByIdInstitucionAndEstadoOrderByFechaSolicitudDesc(idInstitucion, "PENDIENTE")
                .stream().map(SolicitudEliminacionResponse::from).toList();
    }

    public List<SolicitudEliminacionResponse> listarPorEstado(String estado) {
        UUID idInstitucion = TenantContext.getOrThrow();
        return solicitudRepository
                .findAllByIdInstitucionAndEstadoOrderByFechaSolicitudDesc(idInstitucion, estado)
                .stream().map(SolicitudEliminacionResponse::from).toList();
    }

    @Transactional
    public SolicitudEliminacionResponse aprobar(UUID id, String observacion) {
        SolicitudEliminacionDimension s = solicitudRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada"));
        if (!"PENDIENTE".equals(s.getEstado())) {
            throw new IllegalStateException("La solicitud ya fue procesada");
        }
        s.setEstado("APROBADA");
        s.setIdUsuarioResolucion(SecurityUtils.currentUserId());
        s.setFechaResolucion(Instant.now());
        if (observacion != null && !observacion.isBlank()) {
            s.setObservacion(observacion);
        }
        periodoDimensionRepository.findByIdPeriodoEvaluacionAndDimensionId(
                s.getIdPeriodoEvaluacion(), s.getIdDimension()).ifPresent(pd -> {
            periodoDimensionRepository.delete(pd);
        });
        return SolicitudEliminacionResponse.from(solicitudRepository.save(s));
    }

    @Transactional
    public SolicitudEliminacionResponse rechazar(UUID id, String observacion) {
        SolicitudEliminacionDimension s = solicitudRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada"));
        if (!"PENDIENTE".equals(s.getEstado())) {
            throw new IllegalStateException("La solicitud ya fue procesada");
        }
        s.setEstado("RECHAZADA");
        s.setIdUsuarioResolucion(SecurityUtils.currentUserId());
        s.setFechaResolucion(Instant.now());
        s.setObservacion(observacion);
        return SolicitudEliminacionResponse.from(solicitudRepository.save(s));
    }
}
