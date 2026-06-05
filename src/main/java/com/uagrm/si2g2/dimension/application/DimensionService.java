package com.uagrm.si2g2.dimension.application;

import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.common.dto.ApiResponse;
import com.uagrm.si2g2.dimension.domain.Dimension;
import com.uagrm.si2g2.dimension.domain.DimensionRepository;
import com.uagrm.si2g2.dimension.domain.PeriodoDimension;
import com.uagrm.si2g2.dimension.domain.PeriodoDimensionRepository;
import com.uagrm.si2g2.dimension.dto.*;
import com.uagrm.si2g2.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DimensionService {

    private final DimensionRepository dimensionRepository;
    private final PeriodoDimensionRepository periodoDimensionRepository;

    public List<DimensionResponse> listarDisponibles() {
        UUID idInstitucion = TenantContext.getOrThrow();
        return dimensionRepository.findDisponiblesParaInstitucion(idInstitucion)
                .stream().map(DimensionResponse::from).toList();
    }

    public List<DimensionResponse> listarGlobales() {
        return dimensionRepository.findAllByEsGlobalTrueAndEstadoOrderByNombreAsc("ACTIVO")
                .stream().map(DimensionResponse::from).toList();
    }

    @Transactional
    public DimensionResponse crearInstitucional(DimensionRequest request) {
        UUID idInstitucion = TenantContext.getOrThrow();
        if (dimensionRepository.existsByIdInstitucionAndNombre(idInstitucion, request.nombre())) {
            throw new IllegalArgumentException("Ya existe una dimensión con ese nombre para esta institución");
        }
        Dimension d = Dimension.builder()
                .idInstitucion(idInstitucion)
                .nombre(request.nombre().trim())
                .descripcion(request.descripcion())
                .pesoDefault(request.pesoDefault() != null ? request.pesoDefault() : 0)
                .estado("ACTIVO")
                .esGlobal(false)
                .build();
        return DimensionResponse.from(dimensionRepository.save(d));
    }

    @Transactional
    public DimensionResponse crearGlobal(DimensionRequest request) {
        if (dimensionRepository.findByEsGlobalTrueAndNombre(request.nombre().trim()).isPresent()) {
            throw new IllegalArgumentException("Ya existe una dimensión global con ese nombre");
        }
        Dimension d = Dimension.builder()
                .idInstitucion(null)
                .nombre(request.nombre().trim())
                .descripcion(request.descripcion())
                .pesoDefault(request.pesoDefault() != null ? request.pesoDefault() : 0)
                .estado("ACTIVO")
                .esGlobal(true)
                .build();
        return DimensionResponse.from(dimensionRepository.save(d));
    }

    @Transactional
    public DimensionResponse actualizar(UUID id, DimensionRequest request) {
        Dimension d = dimensionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Dimensión no encontrada"));
        d.setNombre(request.nombre().trim());
        d.setDescripcion(request.descripcion());
        if (request.pesoDefault() != null) d.setPesoDefault(request.pesoDefault());
        return DimensionResponse.from(dimensionRepository.save(d));
    }

    @Transactional
    public void eliminar(UUID id) {
        Dimension d = dimensionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Dimensión no encontrada"));
        d.setEstado("INACTIVO");
        dimensionRepository.save(d);
    }

    public List<PeriodoDimensionResponse> listarPesosPeriodo(UUID idPeriodoEvaluacion) {
        return periodoDimensionRepository.findAllByIdPeriodoEvaluacion(idPeriodoEvaluacion)
                .stream().map(PeriodoDimensionResponse::from).toList();
    }

    @Transactional
    public List<PeriodoDimensionResponse> actualizarPesosPeriodo(UUID idPeriodoEvaluacion,
                                                                  List<PeriodoDimensionRequest> solicitudes) {
        periodoDimensionRepository.deleteAllByIdPeriodoEvaluacion(idPeriodoEvaluacion);
        List<PeriodoDimension> creados = solicitudes.stream().map(req -> {
            Dimension dim = dimensionRepository.findById(req.idDimension())
                    .orElseThrow(() -> new EntityNotFoundException("Dimensión no encontrada: " + req.idDimension()));
            return PeriodoDimension.builder()
                    .idPeriodoEvaluacion(idPeriodoEvaluacion)
                    .dimension(dim)
                    .ponderacion(req.ponderacion())
                    .build();
        }).toList();
        return periodoDimensionRepository.saveAll(creados).stream()
                .map(PeriodoDimensionResponse::from).toList();
    }
}
