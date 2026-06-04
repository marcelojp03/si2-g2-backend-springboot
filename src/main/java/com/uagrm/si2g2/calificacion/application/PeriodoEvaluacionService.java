package com.uagrm.si2g2.calificacion.application;

import com.uagrm.si2g2.academico.domain.GestionAcademica;
import com.uagrm.si2g2.academico.domain.GestionAcademicaRepository;
import com.uagrm.si2g2.calificacion.domain.*;
import com.uagrm.si2g2.calificacion.dto.*;
import com.uagrm.si2g2.common.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
                .stream().map(PeriodoEvaluacionResponse::from).toList();
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
}