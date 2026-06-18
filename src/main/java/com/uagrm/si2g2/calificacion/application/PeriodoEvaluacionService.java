package com.uagrm.si2g2.calificacion.application;

import com.uagrm.si2g2.academico.domain.GestionAcademica;
import com.uagrm.si2g2.academico.domain.GestionAcademicaRepository;
import com.uagrm.si2g2.calificacion.domain.*;
import com.uagrm.si2g2.calificacion.dto.*;
import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.dimension.application.DimensionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PeriodoEvaluacionService {

    private static final String ESTADO_ABIERTO = "ABIERTO";
    private static final String ESTADO_CERRADO = "CERRADO";
    private static final String ESTADO_EN_CIERRE = "EN_CIERRE";
    private static final String ESTADO_REABIERTO = "REABIERTO";

    private static final Map<String, Long> MAX_DIAS_POR_TIPO = Map.of(
            "BIMESTRAL", 62L,
            "TRIMESTRAL", 95L,
            "SEMESTRAL", 185L,
            "ANUAL", 366L
    );

    private final PeriodoEvaluacionRepository periodoRepository;
    private final GestionAcademicaRepository gestionAcademicaRepository;
    private final DimensionService dimensionService;

    @Transactional
    public List<PeriodoEvaluacionResponse> crearPeriodosPorGestion(UUID idGestion, List<PeriodoEvaluacionRequest> periodos) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        GestionAcademica gestion = gestionAcademicaRepository.findByIdAndIdInstitucion(idGestion, idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Gestion academica no encontrada"));

        List<PeriodoEvaluacion> creados = new ArrayList<>();
        for (PeriodoEvaluacionRequest req : periodos) {
            validarDuracion(req.tipoPeriodo(), req.fechaInicio(), req.fechaFin(), gestion);
            PeriodoEvaluacion periodo = PeriodoEvaluacion.builder()
                    .idInstitucion(idInstitucion)
                    .idGestionAcademica(gestion.getId())
                    .numeroPeriodo(req.numeroPeriodo())
                    .tipoPeriodo(req.tipoPeriodo())
                    .fechaInicio(req.fechaInicio())
                    .fechaFin(req.fechaFin())
                    .estado(ESTADO_ABIERTO)
                    .pesoSer(req.pesoSer())
                    .pesoSaber(req.pesoSaber())
                    .pesoHacer(req.pesoHacer())
                    .pesoAuto(req.pesoAuto())
                    .build();
            creados.add(periodoRepository.save(periodo));
        }
        return creados.stream().map(PeriodoEvaluacionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<PeriodoEvaluacionResponse> listarPorGestion(UUID idGestion) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        return periodoRepository.findAllByIdInstitucionAndIdGestionAcademica(idInstitucion, idGestion)
                .stream()
                .sorted(Comparator.comparing(PeriodoEvaluacion::getNumeroPeriodo))
                .map(PeriodoEvaluacionResponse::from).toList();
    }

    @Transactional
    public List<PeriodoEvaluacionResponse> sincronizarPeriodosConfigurados(GestionAcademica gestion) {
        return sincronizarPeriodosConfigurados(gestion, gestion.getTipoPeriodo(), gestion.getCantidadPeriodos());
    }

    @Transactional
    public List<PeriodoEvaluacionResponse> sincronizarPeriodosConfigurados(GestionAcademica gestion, String tipoPeriodo, Integer cantidadPeriodos) {
        String tipo = normalizarTipoPeriodo(tipoPeriodo);
        int cantidad = normalizarCantidadPeriodos(cantidadPeriodos);

        Map<Integer, PeriodoEvaluacion> existentes = periodoRepository
                .findAllByIdInstitucionAndIdGestionAcademica(gestion.getIdInstitucion(), gestion.getId())
                .stream()
                .collect(Collectors.toMap(PeriodoEvaluacion::getNumeroPeriodo, Function.identity(), (a, b) -> a));

        List<PeriodoEvaluacion> guardados = new ArrayList<>();
        for (int numero = 1; numero <= cantidad; numero++) {
            LocalDate inicio = calcularFechaInicio(gestion, cantidad, numero);
            LocalDate fin = calcularFechaFin(gestion, cantidad, numero);
            PeriodoEvaluacion periodo = existentes.get(numero);

            if (periodo == null) {
                PeriodoEvaluacion nuevo = periodoRepository.save(PeriodoEvaluacion.builder()
                        .idInstitucion(gestion.getIdInstitucion())
                        .idGestionAcademica(gestion.getId())
                        .numeroPeriodo(numero)
                        .tipoPeriodo(tipo)
                        .fechaInicio(inicio)
                        .fechaFin(fin)
                        .estado(ESTADO_ABIERTO)
                        .pesoSer(10)
                        .pesoSaber(45)
                        .pesoHacer(40)
                        .pesoAuto(5)
                        .build());
                dimensionService.sincronizarPesosDefaultPeriodo(gestion.getIdInstitucion(), nuevo.getId());
                guardados.add(nuevo);
                continue;
            }

            if (!ESTADO_CERRADO.equals(periodo.getEstado())) {
                periodo.setTipoPeriodo(tipo);
                periodo.setFechaInicio(inicio);
                periodo.setFechaFin(fin);
                guardados.add(periodoRepository.save(periodo));
            } else {
                guardados.add(periodo);
            }
        }

        return guardados.stream()
                .sorted(Comparator.comparing(PeriodoEvaluacion::getNumeroPeriodo))
                .map(PeriodoEvaluacionResponse::from)
                .toList();
    }

    @Transactional
    public void sincronizarGestionesDesdeConfiguracion(UUID idInstitucion, String tipoPeriodo, Integer cantidadPeriodos) {
        String tipo = normalizarTipoPeriodo(tipoPeriodo);
        int cantidad = normalizarCantidadPeriodos(cantidadPeriodos);
        gestionAcademicaRepository.findAllByIdInstitucion(idInstitucion).stream()
                .filter(g -> !"ANULADA".equals(g.getEstado()))
                .forEach(g -> {
                    g.setTipoPeriodo(tipo);
                    g.setCantidadPeriodos(cantidad);
                    gestionAcademicaRepository.save(g);
                    sincronizarPeriodosConfigurados(g, tipo, cantidad);
                });
    }

    @Transactional(readOnly = true)
    public PeriodoEvaluacionResponse obtener(UUID id) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        return periodoRepository.findByIdAndIdInstitucion(id, idInstitucion)
                .map(PeriodoEvaluacionResponse::from)
                .orElseThrow(() -> new EntityNotFoundException("Periodo no encontrado"));
    }

    @Transactional
    public PeriodoEvaluacionResponse actualizarFechas(UUID id, LocalDate fechaInicio, LocalDate fechaFin) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        PeriodoEvaluacion periodo = periodoRepository.findByIdAndIdInstitucion(id, idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Periodo no encontrado"));

        if (!ESTADO_ABIERTO.equals(periodo.getEstado())) {
            throw new IllegalStateException("Solo se pueden editar periodos ABIERTOS");
        }

        GestionAcademica gestion = gestionAcademicaRepository.findByIdAndIdInstitucion(
                periodo.getIdGestionAcademica(), idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Gestion academica no encontrada"));

        validarDuracion(periodo.getTipoPeriodo(), fechaInicio, fechaFin, gestion);

        periodo.setFechaInicio(fechaInicio);
        periodo.setFechaFin(fechaFin);
        return PeriodoEvaluacionResponse.from(periodoRepository.save(periodo));
    }

    @Transactional
    public PeriodoEvaluacionResponse actualizarPesos(UUID id, Integer pesoSer, Integer pesoSaber,
                                                      Integer pesoHacer, Integer pesoAuto) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        PeriodoEvaluacion periodo = periodoRepository.findByIdAndIdInstitucion(id, idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Periodo no encontrado"));

        if (!ESTADO_ABIERTO.equals(periodo.getEstado())) {
            throw new IllegalStateException("Solo se pueden editar pesos de periodos ABIERTOS");
        }

        if (pesoSer != null) periodo.setPesoSer(pesoSer);
        if (pesoSaber != null) periodo.setPesoSaber(pesoSaber);
        if (pesoHacer != null) periodo.setPesoHacer(pesoHacer);
        if (pesoAuto != null) periodo.setPesoAuto(pesoAuto);

        return PeriodoEvaluacionResponse.from(periodoRepository.save(periodo));
    }

    @Transactional
    public PeriodoEvaluacionResponse cerrarPeriodo(UUID id, String justificacion) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        PeriodoEvaluacion periodo = periodoRepository.findByIdAndIdInstitucion(id, idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Periodo no encontrado"));

        if (!ESTADO_ABIERTO.equals(periodo.getEstado())) {
            throw new IllegalStateException("Solo periodos ABIERTOS pueden cerrarse");
        }

        periodo.setEstado(ESTADO_CERRADO);
        periodo.setFechaCierre(java.time.Instant.now());
        periodo.setJustificacionCierre(justificacion);
        periodo.setIdUsuarioCierre(SecurityUtils.currentUserId());

        return PeriodoEvaluacionResponse.from(periodoRepository.save(periodo));
    }

    @Transactional
    public PeriodoEvaluacionResponse reopenPeriodo(UUID id, String justificacion) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        PeriodoEvaluacion periodo = periodoRepository.findByIdAndIdInstitucion(id, idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Periodo no encontrado"));

        if (!ESTADO_CERRADO.equals(periodo.getEstado())) {
            throw new IllegalStateException("Solo periodos CERRADOS pueden reabrirse");
        }

        periodo.setEstado(ESTADO_REABIERTO);
        periodo.setFechaReapertura(java.time.Instant.now());
        periodo.setJustificacionReapertura(justificacion);
        periodo.setIdUsuarioReapertura(SecurityUtils.currentUserId());

        return PeriodoEvaluacionResponse.from(periodoRepository.save(periodo));
    }

    private void validarDuracion(String tipoPeriodo, LocalDate fechaInicio, LocalDate fechaFin,
                                   GestionAcademica gestion) {
        long dias = ChronoUnit.DAYS.between(fechaInicio, fechaFin) + 1;

        Long maxDias = MAX_DIAS_POR_TIPO.get(tipoPeriodo);
        if (maxDias != null && dias > maxDias) {
            throw new IllegalArgumentException(
                    "Un período " + tipoPeriodo + " no puede exceder " + maxDias + " días (tiene " + dias + ")");
        }

        if (fechaInicio.isBefore(gestion.getFechaInicio())) {
            throw new IllegalArgumentException(
                    "La fecha inicio del período (" + fechaInicio + ") no puede ser anterior al inicio de la gestión (" + gestion.getFechaInicio() + ")");
        }

        if (fechaFin.isAfter(gestion.getFechaFin())) {
            throw new IllegalArgumentException(
                    "La fecha fin del período (" + fechaFin + ") no puede ser posterior al fin de la gestión (" + gestion.getFechaFin() + ")");
        }
    }

    private String normalizarTipoPeriodo(String tipoPeriodo) {
        return tipoPeriodo != null && !tipoPeriodo.isBlank() ? tipoPeriodo.trim().toUpperCase() : "BIMESTRAL";
    }

    private int normalizarCantidadPeriodos(Integer cantidadPeriodos) {
        if (cantidadPeriodos == null) return 4;
        return Math.max(1, Math.min(cantidadPeriodos, 12));
    }

    private LocalDate calcularFechaInicio(GestionAcademica gestion, int cantidadPeriodos, int numeroPeriodo) {
        long dias = ChronoUnit.DAYS.between(gestion.getFechaInicio(), gestion.getFechaFin()) + 1;
        long offset = ((long) (numeroPeriodo - 1) * dias) / cantidadPeriodos;
        return gestion.getFechaInicio().plusDays(offset);
    }

    private LocalDate calcularFechaFin(GestionAcademica gestion, int cantidadPeriodos, int numeroPeriodo) {
        if (numeroPeriodo == cantidadPeriodos) return gestion.getFechaFin();
        long dias = ChronoUnit.DAYS.between(gestion.getFechaInicio(), gestion.getFechaFin()) + 1;
        long offsetFinExclusivo = ((long) numeroPeriodo * dias) / cantidadPeriodos;
        return gestion.getFechaInicio().plusDays(offsetFinExclusivo - 1);
    }
}
