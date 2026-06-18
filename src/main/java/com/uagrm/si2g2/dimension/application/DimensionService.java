package com.uagrm.si2g2.dimension.application;

import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.common.dto.ApiResponse;
import com.uagrm.si2g2.calificacion.domain.PeriodoEvaluacion;
import com.uagrm.si2g2.calificacion.domain.PeriodoEvaluacionRepository;
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
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DimensionService {

    private final DimensionRepository dimensionRepository;
    private final PeriodoDimensionRepository periodoDimensionRepository;
    private final PeriodoEvaluacionRepository periodoEvaluacionRepository;

    public List<DimensionResponse> listarDisponibles() {
        UUID idInstitucion = TenantContext.getOrThrow();
        return dimensionRepository.findDisponiblesParaInstitucion(idInstitucion)
                .stream().map(DimensionResponse::from).toList();
    }

    public List<DimensionResponse> listarModeloInstitucional(UUID idInstitucion) {
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
    public List<DimensionResponse> actualizarModeloInstitucional(List<DimensionRequest> requests) {
        UUID idInstitucion = TenantContext.getOrThrow();
        int suma = requests.stream()
                .map(DimensionRequest::pesoDefault)
                .filter(peso -> peso != null && peso > 0)
                .mapToInt(Integer::intValue)
                .sum();
        if (suma != 100) {
            throw new IllegalArgumentException("Las ponderaciones activas del modelo institucional deben sumar 100");
        }
        long nombresUnicos = requests.stream()
                .map(req -> req.nombre().trim().toUpperCase())
                .distinct()
                .count();
        if (nombresUnicos != requests.size()) {
            throw new IllegalArgumentException("No se permiten dimensiones duplicadas en el modelo institucional");
        }

        Map<String, Dimension> existentes = dimensionRepository.findAllByIdInstitucionAndEstadoOrderByNombreAsc(idInstitucion, "ACTIVO")
                .stream()
                .collect(Collectors.toMap(d -> d.getNombre().toUpperCase(), Function.identity(), (a, b) -> a));

        List<Dimension> guardadas = requests.stream().map(req -> {
            String nombre = req.nombre().trim().toUpperCase();
            Dimension dimension = existentes.getOrDefault(nombre, Dimension.builder()
                    .idInstitucion(idInstitucion)
                    .nombre(nombre)
                    .estado("ACTIVO")
                    .esGlobal(false)
                    .build());
            dimension.setDescripcion(req.descripcion());
            dimension.setPesoDefault(req.pesoDefault() != null ? req.pesoDefault() : 0);
            return dimensionRepository.save(dimension);
        }).toList();

        sincronizarModeloEnPeriodosEditables(idInstitucion);

        return guardadas.stream().map(DimensionResponse::from).toList();
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
    public void sincronizarPesosDefaultPeriodo(UUID idInstitucion, UUID idPeriodoEvaluacion) {
        if (!periodoDimensionRepository.findAllByIdPeriodoEvaluacion(idPeriodoEvaluacion).isEmpty()) return;
        crearPesosPeriodoDesdeModelo(idInstitucion, idPeriodoEvaluacion);
    }

    private void sincronizarModeloEnPeriodosEditables(UUID idInstitucion) {
        periodoEvaluacionRepository.findAllByIdInstitucion(idInstitucion).stream()
                .filter(periodo -> !"CERRADO".equals(periodo.getEstado()))
                .forEach(periodo -> {
                    periodoDimensionRepository.deleteAllByIdPeriodoEvaluacion(periodo.getId());
                    crearPesosPeriodoDesdeModelo(idInstitucion, periodo.getId());
                });
    }

    private void crearPesosPeriodoDesdeModelo(UUID idInstitucion, UUID idPeriodoEvaluacion) {
        List<PeriodoDimension> pesos = dimensionRepository.findDisponiblesParaInstitucion(idInstitucion).stream()
                .filter(d -> d.getPesoDefault() != null && d.getPesoDefault() > 0)
                .map(d -> PeriodoDimension.builder()
                        .idPeriodoEvaluacion(idPeriodoEvaluacion)
                        .dimension(d)
                        .ponderacion(d.getPesoDefault())
                        .build())
                .toList();
        if (!pesos.isEmpty()) {
            periodoDimensionRepository.saveAll(pesos);
        }
    }

    @Transactional
    public List<PeriodoDimensionResponse> actualizarPesosPeriodo(UUID idPeriodoEvaluacion,
                                                                   List<PeriodoDimensionRequest> solicitudes) {
        int suma = solicitudes.stream().mapToInt(PeriodoDimensionRequest::ponderacion).sum();
        if (suma != 100) {
            throw new IllegalArgumentException("Las ponderaciones del período deben sumar 100");
        }
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
