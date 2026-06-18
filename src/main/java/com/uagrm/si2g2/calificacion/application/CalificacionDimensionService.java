package com.uagrm.si2g2.calificacion.application;

import com.uagrm.si2g2.calificacion.domain.*;
import com.uagrm.si2g2.calificacion.dto.*;
import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.dimension.domain.PeriodoDimension;
import com.uagrm.si2g2.dimension.domain.PeriodoDimensionRepository;
import com.uagrm.si2g2.docente.domain.DocenteRepository;
import com.uagrm.si2g2.estudiante.domain.Estudiante;
import com.uagrm.si2g2.estudiante.domain.EstudianteRepository;
import com.uagrm.si2g2.materia.domain.MateriaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CalificacionDimensionService {

    private static final BigDecimal PESO_SER = new BigDecimal("10");
    private static final BigDecimal PESO_SABER = new BigDecimal("45");
    private static final BigDecimal PESO_HACER = new BigDecimal("40");
    private static final BigDecimal PESO_AUTO = new BigDecimal("5");
    private static final BigDecimal NOTA_APROBACION = new BigDecimal("51");
    private static final BigDecimal PUNTAJE_MAXIMO = new BigDecimal("100");

    private final PeriodoEvaluacionRepository periodoRepository;
    private final ActividadEvaluativaRepository actividadRepository;
    private final CalificacionActividadRepository calificacionActividadRepository;
    private final CalificacionSerRepository calificacionSerRepository;
    private final AutoevaluacionTrimestralRepository autoevaluacionRepository;
    private final ObservacionSerRepository observacionSerRepository;
    private final EstudianteRepository estudianteRepository;
    private final MateriaRepository materiaRepository;
    private final DocenteRepository docenteRepository;
    private final PeriodoDimensionRepository periodoDimensionRepository;

    public UUID obtenerDocenteId(UUID idUsuario) {
        return docenteRepository.findByIdUsuarioAndIdInstitucion(idUsuario, SecurityUtils.requireCurrentInstitutionId())
                .map(doc -> doc.getId())
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<ActividadEvaluativaResponse> listarActividadesPorPeriodo(UUID idPeriodo, String dimension) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        List<ActividadEvaluativa> actividades;
        if (dimension != null && !dimension.isBlank()) {
            actividades = actividadRepository.findAllByIdInstitucionAndIdPeriodoEvaluacionAndDimension(idInstitucion, idPeriodo, dimension);
        } else {
            actividades = actividadRepository.findAllByIdInstitucionAndIdPeriodoEvaluacion(idInstitucion, idPeriodo);
        }
        return actividades.stream().map(ActividadEvaluativaResponse::from).toList();
    }

    @Transactional
    public ActividadEvaluativaResponse crearActividad(UUID idPeriodo, ActividadEvaluativaRequest request) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        PeriodoEvaluacion periodo = periodoRepository.findByIdAndIdInstitucion(idPeriodo, idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Periodo no encontrado"));

        if (!"BORRADOR".equals(periodo.getEstado()) && !"REABIERTO".equals(periodo.getEstado())) {
            throw new IllegalStateException("No se puede crear actividades en periodo " + periodo.getEstado());
        }

        ActividadEvaluativa actividad = ActividadEvaluativa.builder()
                .idInstitucion(idInstitucion)
                .idPeriodoEvaluacion(idPeriodo)
                .idMateria(request.idMateria())
                .idDocente(request.idDocente())
                .nombreActividad(request.nombreActividad())
                .dimension(request.dimension())
                .fechaActividad(request.fechaActividad())
                .descripcionEvidencia(request.descripcionEvidencia())
                .puntajeMaximo(request.puntajeMaximo() != null ? request.puntajeMaximo() : 100)
                .estado(request.estado() != null ? request.estado() : "BORRADOR")
                .build();

        return ActividadEvaluativaResponse.from(actividadRepository.save(actividad));
    }

    @Transactional
    public ActividadEvaluativaResponse actualizarActividad(UUID id, ActividadEvaluativaRequest request) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        ActividadEvaluativa actividad = actividadRepository.findByIdAndIdInstitucion(id, idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Actividad no encontrada"));

        actividad.setNombreActividad(request.nombreActividad());
        actividad.setDimension(request.dimension());
        actividad.setFechaActividad(request.fechaActividad());
        actividad.setDescripcionEvidencia(request.descripcionEvidencia());
        if (request.puntajeMaximo() != null) {
            actividad.setPuntajeMaximo(request.puntajeMaximo());
        }
        if (request.estado() != null) {
            actividad.setEstado(request.estado());
            if ("PUBLICADA".equals(request.estado())) {
                actividad.setPublicadoEn(java.time.Instant.now());
            }
        }

        return ActividadEvaluativaResponse.from(actividadRepository.save(actividad));
    }

    @Transactional
    public void eliminarActividad(UUID id) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        ActividadEvaluativa actividad = actividadRepository.findByIdAndIdInstitucion(id, idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Actividad no encontrada"));
        actividadRepository.delete(actividad);
    }

    @Transactional
    public List<CalificacionActividadResponse> registrarCalificacionesActividad(CalificacionActividadRegistroRequest request) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        ActividadEvaluativa actividad = actividadRepository.findByIdAndIdInstitucion(request.idActividad(), idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Actividad no encontrada"));

        List<CalificacionActividad> resultados = new java.util.ArrayList<>();
        for (CalificacionActividadDetalleRequest detalle : request.detalles()) {
            CalificacionActividad calificacion = calificacionActividadRepository
                    .findByIdInstitucionAndIdActividadAndIdEstudiante(idInstitucion, request.idActividad(), detalle.idEstudiante())
                    .orElseGet(() -> CalificacionActividad.builder()
                            .idInstitucion(idInstitucion)
                            .idActividad(request.idActividad())
                            .idEstudiante(detalle.idEstudiante())
                            .build());

            if (detalle.notaObtenida() != null) {
                calificacion.setNotaObtenida(detalle.notaObtenida());
                calificacion.setEstado("REGISTRADA");
            }
            if (detalle.observacion() != null) {
                calificacion.setObservacion(detalle.observacion());
            }
            if (calificacion.getId() == null) {
                calificacion.setIdUsuarioRegistro(SecurityUtils.currentUserId());
            }
            calificacion.setIdUsuarioModificacion(SecurityUtils.currentUserId());

            resultados.add(calificacionActividadRepository.save(calificacion));
        }
        return resultados.stream().map(CalificacionActividadResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<CalificacionActividadResponse> listarCalificacionesActividad(UUID idActividad) {
        return calificacionActividadRepository.findAllByIdActividad(idActividad)
                .stream().map(CalificacionActividadResponse::from).toList();
    }

    public BigDecimal calcularSaberPorEstudiante(UUID idPeriodo, UUID idMateria, UUID idEstudiante) {
        List<ActividadEvaluativa> actividades = actividadRepository
                .findAllByIdInstitucionAndIdPeriodoEvaluacionAndIdMateriaAndDimension(
                        SecurityUtils.requireCurrentInstitutionId(), idPeriodo, idMateria, "SABER");

        if (actividades.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal suma = BigDecimal.ZERO;
        int count = 0;
        for (ActividadEvaluativa act : actividades) {
            var opt = calificacionActividadRepository.findByIdActividadAndIdEstudiante(act.getId(), idEstudiante);
            if (opt.isPresent() && opt.get().getNotaObtenida() != null) {
                suma = suma.add(opt.get().getNotaObtenida());
                count++;
            }
        }

        if (count == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal promedio = suma.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
        return promedio.multiply(pesoDimension(idPeriodo, "SABER", PESO_SABER)).divide(PUNTAJE_MAXIMO, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calcularHacerPorEstudiante(UUID idPeriodo, UUID idMateria, UUID idEstudiante) {
        List<ActividadEvaluativa> actividades = actividadRepository
                .findAllByIdInstitucionAndIdPeriodoEvaluacionAndIdMateriaAndDimension(
                        SecurityUtils.requireCurrentInstitutionId(), idPeriodo, idMateria, "HACER");

        if (actividades.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal suma = BigDecimal.ZERO;
        int count = 0;

        for (ActividadEvaluativa act : actividades) {
            var opt = calificacionActividadRepository.findByIdActividadAndIdEstudiante(act.getId(), idEstudiante);
            if (opt.isPresent() && opt.get().getNotaObtenida() != null) {
                suma = suma.add(opt.get().getNotaObtenida());
                count++;
            }
        }

        if (count == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal promedio = suma.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
        return promedio.multiply(pesoDimension(idPeriodo, "HACER", PESO_HACER)).divide(PUNTAJE_MAXIMO, 2, RoundingMode.HALF_UP);
    }

    @Transactional
    public CalificacionSerResponse guardarSer(UUID idPeriodo, CalificacionSerRequest request) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();

        BigDecimal pesoSer = pesoDimension(idPeriodo, "SER", PESO_SER);
        if (request.notaSer().compareTo(pesoSer) > 0) {
            throw new IllegalArgumentException("La nota SER no puede exceder " + pesoSer + " puntos");
        }

        CalificacionSer calificacion = calificacionSerRepository
                .findByIdInstitucionAndIdPeriodoEvaluacionAndIdMateriaAndIdEstudiante(
                        idInstitucion, idPeriodo, request.idMateria(), request.idEstudiante())
                .orElseGet(() -> CalificacionSer.builder()
                        .idInstitucion(idInstitucion)
                        .idPeriodoEvaluacion(idPeriodo)
                        .idMateria(request.idMateria())
                        .idEstudiante(request.idEstudiante())
                        .build());

        calificacion.setNotaSer(request.notaSer());
        calificacion.setObservacionFinal(request.observacionFinal());
        calificacion.setEstado("REGISTRADA");
        if (calificacion.getId() == null) {
            calificacion.setIdUsuarioRegistro(SecurityUtils.currentUserId());
        }
        calificacion.setIdUsuarioModificacion(SecurityUtils.currentUserId());

        return CalificacionSerResponse.from(calificacionSerRepository.save(calificacion));
    }

    @Transactional(readOnly = true)
    public CalificacionSerResponse obtenerSer(UUID idPeriodo, UUID idEstudiante, UUID idMateria) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        return calificacionSerRepository
                .findByIdInstitucionAndIdPeriodoEvaluacionAndIdMateriaAndIdEstudiante(idInstitucion, idPeriodo, idMateria, idEstudiante)
                .map(CalificacionSerResponse::from)
                .orElse(null);
    }

    @Transactional
    public ObservacionSerResponse agregarObservacionSer(UUID idPeriodo, UUID idDocente, ObservacionSerRequest request) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();

        ObservacionSer observacion = ObservacionSer.builder()
                .idInstitucion(idInstitucion)
                .idPeriodoEvaluacion(idPeriodo)
                .idEstudiante(request.idEstudiante())
                .idDocente(idDocente)
                .idMateria(request.idMateria())
                .fechaObservacion(request.fechaObservacion())
                .comportamiento(request.comportamiento())
                .descripcion(request.descripcion())
                .build();

        return ObservacionSerResponse.from(observacionSerRepository.save(observacion));
    }

    @Transactional(readOnly = true)
    public List<ObservacionSerResponse> listarObservacionesSer(UUID idPeriodo, UUID idEstudiante, UUID idMateria) {
        return observacionSerRepository
                .findAllByIdInstitucionAndIdPeriodoEvaluacionAndIdEstudianteAndIdMateria(
                        SecurityUtils.requireCurrentInstitutionId(), idPeriodo, idEstudiante, idMateria)
                .stream().map(ObservacionSerResponse::from).toList();
    }

    @Transactional
    public AutoevaluacionTrimestralResponse guardarAutoevaluacion(UUID idPeriodo, AutoevaluacionTrimestralRequest request) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();

        BigDecimal pesoAuto = pesoDimension(idPeriodo, "AUTOEVALUACION", PESO_AUTO);
        if (request.notaAutoevaluacion().compareTo(pesoAuto) > 0) {
            throw new IllegalArgumentException("La nota de autoevaluación no puede exceder " + pesoAuto + " puntos");
        }

        AutoevaluacionTrimestral autoevaluacion = autoevaluacionRepository
                .findByIdInstitucionAndIdPeriodoEvaluacionAndIdMateriaAndIdEstudiante(
                        idInstitucion, idPeriodo, request.idMateria(), request.idEstudiante())
                .orElseGet(() -> AutoevaluacionTrimestral.builder()
                        .idInstitucion(idInstitucion)
                        .idPeriodoEvaluacion(idPeriodo)
                        .idMateria(request.idMateria())
                        .idEstudiante(request.idEstudiante())
                        .build());

        autoevaluacion.setNotaAutoevaluacion(request.notaAutoevaluacion());
        autoevaluacion.setComentario(request.comentario());
        autoevaluacion.setEstado("REGISTRADA");
        if (autoevaluacion.getId() == null) {
            autoevaluacion.setIdUsuarioRegistro(SecurityUtils.currentUserId());
        }
        autoevaluacion.setIdUsuarioModificacion(SecurityUtils.currentUserId());

        return AutoevaluacionTrimestralResponse.from(autoevaluacionRepository.save(autoevaluacion));
    }

    @Transactional(readOnly = true)
    public AutoevaluacionTrimestralResponse obtenerAutoevaluacion(UUID idPeriodo, UUID idEstudiante, UUID idMateria) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        return autoevaluacionRepository
                .findByIdInstitucionAndIdPeriodoEvaluacionAndIdMateriaAndIdEstudiante(idInstitucion, idPeriodo, idMateria, idEstudiante)
                .map(AutoevaluacionTrimestralResponse::from)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public ConsolidadoEstudianteResponse obtenerConsolidadoEstudiante(UUID idPeriodo, UUID idEstudiante, UUID idMateria) {
        BigDecimal ser = BigDecimal.ZERO;
        BigDecimal auto = BigDecimal.ZERO;

        var optSer = calificacionSerRepository.findByIdInstitucionAndIdPeriodoEvaluacionAndIdMateriaAndIdEstudiante(
                SecurityUtils.requireCurrentInstitutionId(), idPeriodo, idMateria, idEstudiante);
        if (optSer.isPresent()) {
            ser = optSer.get().getNotaSer();
        }

        var optAuto = autoevaluacionRepository.findByIdInstitucionAndIdPeriodoEvaluacionAndIdMateriaAndIdEstudiante(
                SecurityUtils.requireCurrentInstitutionId(), idPeriodo, idMateria, idEstudiante);
        if (optAuto.isPresent()) {
            auto = optAuto.get().getNotaAutoevaluacion();
        }

        BigDecimal saber = calcularSaberPorEstudiante(idPeriodo, idMateria, idEstudiante);
        BigDecimal hacer = calcularHacerPorEstudiante(idPeriodo, idMateria, idEstudiante);

        Estudiante estudiante = estudianteRepository.findById(idEstudiante).orElse(null);
        String nombreEstudiante = (estudiante != null) ? estudiante.getNombres() + " " + estudiante.getApellidos() : "";

        return ConsolidadoEstudianteResponse.calcular(idEstudiante, nombreEstudiante, saber, hacer, ser, auto);
    }

    private BigDecimal pesoDimension(UUID idPeriodo, String nombre, BigDecimal fallback) {
        String normalizado = nombre.toUpperCase();
        return periodoDimensionRepository.findAllByIdPeriodoEvaluacion(idPeriodo).stream()
                .filter(pd -> nombreCoincide(pd, normalizado))
                .findFirst()
                .map(pd -> BigDecimal.valueOf(pd.getPonderacion()))
                .orElse(fallback);
    }

    private boolean nombreCoincide(PeriodoDimension periodoDimension, String nombre) {
        String actual = periodoDimension.getDimension().getNombre().toUpperCase();
        if (actual.equals(nombre)) return true;
        return nombre.equals("AUTOEVALUACION") && actual.equals("AUTO");
    }
}
