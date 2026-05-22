package com.uagrm.si2g2.calificacion.application;

import com.uagrm.si2g2.academico.domain.GestionAcademica;
import com.uagrm.si2g2.academico.domain.GestionAcademicaRepository;
import com.uagrm.si2g2.asignacion.domain.AsignacionDocente;
import com.uagrm.si2g2.asignacion.domain.AsignacionDocenteRepository;
import com.uagrm.si2g2.auditoria.application.AuditoriaService;
import com.uagrm.si2g2.calificacion.domain.*;
import com.uagrm.si2g2.calificacion.dto.*;
import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.curso.domain.Curso;
import com.uagrm.si2g2.curso.domain.CursoRepository;
import com.uagrm.si2g2.curso.domain.Paralelo;
import com.uagrm.si2g2.curso.domain.ParaleloRepository;
import com.uagrm.si2g2.docente.domain.Docente;
import com.uagrm.si2g2.docente.domain.DocenteRepository;
import com.uagrm.si2g2.estudiante.domain.Estudiante;
import com.uagrm.si2g2.estudiante.domain.EstudianteRepository;
import com.uagrm.si2g2.inscripcion.domain.Inscripcion;
import com.uagrm.si2g2.inscripcion.domain.InscripcionRepository;
import com.uagrm.si2g2.institucion.application.ConfiguracionService;
import com.uagrm.si2g2.materia.domain.Materia;
import com.uagrm.si2g2.materia.domain.MateriaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalificacionService {

    private static final String ESTADO_ASIGNACION_ACTIVA = "ACTIVA";
    private static final String ESTADO_INSCRIPCION_ACTIVA = "ACTIVA";
    private static final Set<String> ESCALAS_VALIDAS = Set.of("NUMERICA", "LITERAL");
    private static final Set<String> ESTADOS_EVALUACION_VALIDOS = Set.of("ABIERTA", "CERRADA", "ANULADA");
    private static final Set<String> NOTAS_LITERAL_VALIDAS = Set.of("A", "B", "C", "D", "F");

    private final EvaluacionRepository evaluacionRepository;
    private final CalificacionRepository calificacionRepository;
    private final CalificacionCambioRepository cambioRepository;

    private final AsignacionDocenteRepository asignacionDocenteRepository;
    private final InscripcionRepository inscripcionRepository;
    private final EstudianteRepository estudianteRepository;
    private final DocenteRepository docenteRepository;
    private final MateriaRepository materiaRepository;
    private final ParaleloRepository paraleloRepository;
    private final CursoRepository cursoRepository;
    private final GestionAcademicaRepository gestionAcademicaRepository;
    private final ConfiguracionService configuracionService;
    private final AuditoriaService auditoriaService;

    @Transactional(readOnly = true)
    public List<CalificacionAsignacionResponse> listarMisAsignaciones() {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();

        List<AsignacionDocente> asignaciones;
        if (SecurityUtils.currentUserHasRole("DOCENTE")) {
            Docente docente = docenteRepository.findByIdUsuarioAndIdInstitucion(SecurityUtils.currentUserId(), idInstitucion)
                    .orElseThrow(() -> new EntityNotFoundException("No existe un docente asociado al usuario autenticado"));

            asignaciones = asignacionDocenteRepository
                    .findAllByIdInstitucionAndIdDocente(idInstitucion, docente.getId())
                    .stream()
                    .filter(a -> ESTADO_ASIGNACION_ACTIVA.equals(a.getEstado()))
                    .toList();
        } else {
            asignaciones = asignacionDocenteRepository.findAllByIdInstitucion(idInstitucion)
                    .stream()
                    .filter(a -> ESTADO_ASIGNACION_ACTIVA.equals(a.getEstado()))
                    .toList();
        }

        return asignaciones.stream()
                .map(this::toAsignacionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EvaluacionResponse> listarEvaluaciones(UUID idAsignacionDocente, Integer periodo) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        AsignacionDocente asignacion = buscarAsignacionActiva(idAsignacionDocente, idInstitucion);
        validarAccesoLectura(asignacion);

        List<Evaluacion> evaluaciones = periodo == null
                ? evaluacionRepository.findAllByIdInstitucionAndIdAsignacionDocente(idInstitucion, idAsignacionDocente)
                : evaluacionRepository.findAllByIdInstitucionAndIdAsignacionDocenteAndPeriodo(idInstitucion, idAsignacionDocente, periodo);

        return evaluaciones.stream()
                .sorted(Comparator.comparing(Evaluacion::getPeriodo)
                        .thenComparing(Evaluacion::getNombre))
                .map(EvaluacionResponse::from)
                .toList();
    }

    @Transactional
    public EvaluacionResponse crearEvaluacion(EvaluacionRequest request) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        AsignacionDocente asignacion = buscarAsignacionActiva(request.getIdAsignacionDocente(), idInstitucion);
        validarAccesoEscritura(asignacion);
        validarPeriodo(idInstitucion, request.getPeriodo());

        String nombre = normalizarTexto(request.getNombre(), "El nombre de la evaluacion es obligatorio");
        String tipo = normalizarTexto(request.getTipo(), "El tipo de evaluacion es obligatorio");
        String escala = normalizarEscala(request.getEscala());
        BigDecimal ponderacion = normalizarPonderacion(request.getPonderacion());

        if (evaluacionRepository.existsByIdInstitucionAndIdAsignacionDocenteAndPeriodoAndNombreIgnoreCase(
                idInstitucion, asignacion.getId(), request.getPeriodo(), nombre)) {
            throw new IllegalStateException("Ya existe una evaluacion con ese nombre en el periodo seleccionado");
        }

        validarPonderacionTotal(idInstitucion, asignacion.getId(), request.getPeriodo(), ponderacion, null);

        Evaluacion evaluacion = Evaluacion.builder()
                .idInstitucion(idInstitucion)
                .idAsignacionDocente(asignacion.getId())
                .creadoPor(SecurityUtils.currentUserId())
                .periodo(request.getPeriodo())
                .tipo(tipo)
                .nombre(nombre)
                .ponderacion(ponderacion)
                .escala(escala)
                .estado("ABIERTA")
                .build();

        Evaluacion saved = evaluacionRepository.save(evaluacion);
        auditoriaService.registrar(idInstitucion, SecurityUtils.currentUserId(), "CALIFICACIONES",
                "CREAR_EVALUACION", "evaluacion", saved.getId().toString(), true,
                "Evaluacion creada");

        return EvaluacionResponse.from(saved);
    }

    @Transactional
    public EvaluacionResponse actualizarEvaluacion(UUID id, EvaluacionRequest request) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        Evaluacion evaluacion = buscarEvaluacion(id, idInstitucion);
        AsignacionDocente asignacion = buscarAsignacionActiva(evaluacion.getIdAsignacionDocente(), idInstitucion);
        validarAccesoEscritura(asignacion);

        Map<String, Object> antes = Map.of(
                "tipo", evaluacion.getTipo(),
                "nombre", evaluacion.getNombre(),
                "ponderacion", evaluacion.getPonderacion(),
                "escala", evaluacion.getEscala(),
                "estado", evaluacion.getEstado()
        );

        String nombre = normalizarTexto(request.getNombre(), "El nombre de la evaluacion es obligatorio");
        String tipo = normalizarTexto(request.getTipo(), "El tipo de evaluacion es obligatorio");
        String escala = normalizarEscala(request.getEscala());
        String estado = normalizarEstadoEvaluacion(request.getEstado());
        BigDecimal ponderacion = normalizarPonderacion(request.getPonderacion());

        validarPonderacionTotal(idInstitucion, asignacion.getId(), evaluacion.getPeriodo(), ponderacion, evaluacion.getId());

        evaluacion.setTipo(tipo);
        evaluacion.setNombre(nombre);
        evaluacion.setPonderacion(ponderacion);
        evaluacion.setEscala(escala);
        evaluacion.setEstado(estado);

        Evaluacion saved = evaluacionRepository.save(evaluacion);
        auditoriaService.registrarDetallado(idInstitucion, SecurityUtils.currentUserId(), "CALIFICACIONES",
                "ACTUALIZAR_EVALUACION", "evaluacion", saved.getId().toString(), antes,
                Map.of("tipo", saved.getTipo(), "nombre", saved.getNombre(), "ponderacion", saved.getPonderacion(),
                        "escala", saved.getEscala(), "estado", saved.getEstado()),
                true, "Evaluacion actualizada");

        return EvaluacionResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public CalificacionPlantillaResponse obtenerPlantilla(UUID idEvaluacion) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        Evaluacion evaluacion = buscarEvaluacion(idEvaluacion, idInstitucion);
        AsignacionDocente asignacion = buscarAsignacionActiva(evaluacion.getIdAsignacionDocente(), idInstitucion);
        validarAccesoLectura(asignacion);

        List<Inscripcion> inscripciones = obtenerInscripcionesActivasDeAsignacion(idInstitucion, asignacion);
        Map<UUID, Calificacion> calificacionesPorInscripcion = calificacionRepository.findAllByIdEvaluacion(evaluacion.getId())
                .stream()
                .collect(Collectors.toMap(Calificacion::getIdInscripcion, Function.identity()));

        List<CalificacionEstudianteResponse> estudiantes = construirEstudiantesResponse(inscripciones, calificacionesPorInscripcion);

        return CalificacionPlantillaResponse.builder()
                .idEvaluacion(evaluacion.getId())
                .evaluacion(EvaluacionResponse.from(evaluacion))
                .asignacion(toAsignacionResponse(asignacion))
                .estudiantes(estudiantes)
                .totalEstudiantes(estudiantes.size())
                .escalaMaxima(escalaMaxima(idInstitucion))
                .puedeEditar(puedeEditarEvaluacion(evaluacion))
                .build();
    }

    @Transactional
    public CalificacionPlantillaResponse guardarCalificaciones(CalificacionRegistroRequest request) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        Evaluacion evaluacion = buscarEvaluacion(request.getIdEvaluacion(), idInstitucion);
        AsignacionDocente asignacion = buscarAsignacionActiva(evaluacion.getIdAsignacionDocente(), idInstitucion);
        validarAccesoEscritura(asignacion);
        validarEvaluacionEditable(evaluacion);

        List<Inscripcion> inscripciones = obtenerInscripcionesActivasDeAsignacion(idInstitucion, asignacion);
        Set<UUID> idsInscripcionesValidas = inscripciones.stream().map(Inscripcion::getId).collect(Collectors.toSet());
        Set<UUID> idsRecibidos = new HashSet<>();

        for (CalificacionDetalleRequest detalle : request.getDetalles()) {
            if (detalle.getIdInscripcion() == null) {
                throw new IllegalArgumentException("La inscripcion es obligatoria para cada calificacion");
            }
            if (!idsRecibidos.add(detalle.getIdInscripcion())) {
                throw new IllegalArgumentException("La inscripcion esta duplicada en la carga de calificaciones");
            }
            if (!idsInscripcionesValidas.contains(detalle.getIdInscripcion())) {
                throw new IllegalArgumentException("La inscripcion no pertenece al paralelo y gestion de la evaluacion");
            }
        }

        BigDecimal maximo = escalaMaxima(idInstitucion);
        List<Calificacion> guardadas = new ArrayList<>();

        for (CalificacionDetalleRequest detalle : request.getDetalles()) {
            ValorNota valorNuevo = normalizarNota(evaluacion, detalle, maximo);
            Calificacion calificacion = calificacionRepository
                    .findByIdEvaluacionAndIdInscripcion(evaluacion.getId(), detalle.getIdInscripcion())
                    .orElse(null);

            if (calificacion == null) {
                calificacion = Calificacion.builder()
                        .idInstitucion(idInstitucion)
                        .idEvaluacion(evaluacion.getId())
                        .idInscripcion(detalle.getIdInscripcion())
                        .registradoPor(SecurityUtils.currentUserId())
                        .notaNumerica(valorNuevo.notaNumerica())
                        .notaLiteral(valorNuevo.notaLiteral())
                        .build();
                guardadas.add(calificacionRepository.save(calificacion));
            } else {
                ValorNota valorAnterior = ValorNota.from(calificacion);
                if (!valorAnterior.valorTexto().equals(valorNuevo.valorTexto())) {
                    if (detalle.getRazonCambio() == null || detalle.getRazonCambio().isBlank()) {
                        throw new IllegalArgumentException("La razon de cambio es obligatoria al modificar una calificacion");
                    }
                    calificacion.setNotaNumerica(valorNuevo.notaNumerica());
                    calificacion.setNotaLiteral(valorNuevo.notaLiteral());
                    calificacion.setRegistradoPor(SecurityUtils.currentUserId());
                    Calificacion saved = calificacionRepository.save(calificacion);
                    guardadas.add(saved);

                    cambioRepository.save(CalificacionCambio.builder()
                            .idInstitucion(idInstitucion)
                            .idCalificacion(saved.getId())
                            .idUsuario(SecurityUtils.currentUserId())
                            .valorAnterior(valorAnterior.valorTexto())
                            .valorNuevo(valorNuevo.valorTexto())
                            .razon(detalle.getRazonCambio().trim())
                            .build());
                }
            }
        }

        auditoriaService.registrar(idInstitucion, SecurityUtils.currentUserId(), "CALIFICACIONES",
                "GUARDAR_NOTAS", "evaluacion", evaluacion.getId().toString(), true,
                "Calificaciones guardadas: " + guardadas.size());

        return obtenerPlantilla(evaluacion.getId());
    }

    @Transactional(readOnly = true)
    public CalificacionResumenResponse obtenerResumen(UUID idAsignacionDocente, Integer periodo) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        AsignacionDocente asignacion = buscarAsignacionActiva(idAsignacionDocente, idInstitucion);
        validarAccesoLectura(asignacion);
        validarPeriodo(idInstitucion, periodo);

        List<Evaluacion> evaluaciones = evaluacionRepository
                .findAllByIdInstitucionAndIdAsignacionDocenteAndPeriodo(idInstitucion, idAsignacionDocente, periodo)
                .stream()
                .filter(e -> !"ANULADA".equals(e.getEstado()))
                .toList();

        List<UUID> idsEvaluaciones = evaluaciones.stream().map(Evaluacion::getId).toList();
        Map<UUID, Evaluacion> evaluacionesPorId = evaluaciones.stream()
                .collect(Collectors.toMap(Evaluacion::getId, Function.identity()));

        List<Calificacion> calificaciones = idsEvaluaciones.isEmpty()
                ? List.of()
                : calificacionRepository.findAllByIdEvaluacionIn(idsEvaluaciones);

        Map<UUID, List<Calificacion>> calificacionesPorInscripcion = calificaciones.stream()
                .collect(Collectors.groupingBy(Calificacion::getIdInscripcion));

        List<Inscripcion> inscripciones = obtenerInscripcionesActivasDeAsignacion(idInstitucion, asignacion);
        Map<UUID, Estudiante> estudiantes = estudiantesPorId(inscripciones);
        BigDecimal notaMinima = BigDecimal.valueOf(configuracionService.getInt(idInstitucion, "NOTA_MINIMA_APROBACION"));

        List<CalificacionResumenEstudianteResponse> resumenEstudiantes = inscripciones.stream()
                .map(inscripcion -> construirResumenEstudiante(
                        inscripcion,
                        estudiantes.get(inscripcion.getIdEstudiante()),
                        calificacionesPorInscripcion.getOrDefault(inscripcion.getId(), List.of()),
                        evaluacionesPorId,
                        notaMinima
                ))
                .sorted(Comparator.comparing(CalificacionResumenEstudianteResponse::getNombreCompleto))
                .toList();

        BigDecimal ponderacionTotal = evaluaciones.stream()
                .map(Evaluacion::getPonderacion)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CalificacionResumenResponse.builder()
                .idAsignacionDocente(idAsignacionDocente)
                .periodo(periodo)
                .ponderacionTotal(ponderacionTotal)
                .notaMinimaAprobacion(notaMinima)
                .evaluaciones(evaluaciones.stream().map(EvaluacionResponse::from).toList())
                .estudiantes(resumenEstudiantes)
                .build();
    }

    private CalificacionResumenEstudianteResponse construirResumenEstudiante(
            Inscripcion inscripcion,
            Estudiante estudiante,
            List<Calificacion> calificaciones,
            Map<UUID, Evaluacion> evaluacionesPorId,
            BigDecimal notaMinima
    ) {
        if (estudiante == null) {
            throw new EntityNotFoundException("Estudiante no encontrado: " + inscripcion.getIdEstudiante());
        }

        BigDecimal acumulado = BigDecimal.ZERO;
        BigDecimal ponderacionRegistrada = BigDecimal.ZERO;

        for (Calificacion calificacion : calificaciones) {
            Evaluacion evaluacion = evaluacionesPorId.get(calificacion.getIdEvaluacion());
            if (evaluacion == null || calificacion.getNotaNumerica() == null || !"NUMERICA".equals(evaluacion.getEscala())) {
                continue;
            }
            acumulado = acumulado.add(calificacion.getNotaNumerica()
                    .multiply(evaluacion.getPonderacion())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            ponderacionRegistrada = ponderacionRegistrada.add(evaluacion.getPonderacion());
        }

        return CalificacionResumenEstudianteResponse.builder()
                .idInscripcion(inscripcion.getId())
                .idEstudiante(estudiante.getId())
                .codigoEstudiante(estudiante.getCodigoEstudiante())
                .nombreCompleto(estudiante.getNombres() + " " + estudiante.getApellidos())
                .notaConsolidada(acumulado)
                .ponderacionRegistrada(ponderacionRegistrada)
                .estadoAcademico(acumulado.compareTo(notaMinima) >= 0 ? "APROBADO" : "EN_RIESGO")
                .build();
    }

    private Evaluacion buscarEvaluacion(UUID idEvaluacion, UUID idInstitucion) {
        return evaluacionRepository.findByIdAndIdInstitucion(idEvaluacion, idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Evaluacion no encontrada: " + idEvaluacion));
    }

    private AsignacionDocente buscarAsignacionActiva(UUID idAsignacionDocente, UUID idInstitucion) {
        AsignacionDocente asignacion = asignacionDocenteRepository.findByIdAndIdInstitucion(idAsignacionDocente, idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Asignacion docente no encontrada: " + idAsignacionDocente));
        if (!ESTADO_ASIGNACION_ACTIVA.equals(asignacion.getEstado())) {
            throw new IllegalStateException("La asignacion docente no esta activa");
        }
        return asignacion;
    }

    private List<Inscripcion> obtenerInscripcionesActivasDeAsignacion(UUID idInstitucion, AsignacionDocente asignacion) {
        return inscripcionRepository
                .findAllByIdInstitucionAndIdParalelo(idInstitucion, asignacion.getIdParalelo())
                .stream()
                .filter(i -> asignacion.getIdGestion().equals(i.getIdGestion()))
                .filter(i -> ESTADO_INSCRIPCION_ACTIVA.equals(i.getEstado()))
                .sorted(Comparator.comparing(i -> i.getId().toString()))
                .toList();
    }

    private void validarAccesoLectura(AsignacionDocente asignacion) {
        if (SecurityUtils.currentUserHasRole("ADMIN_INSTITUCION")
                || SecurityUtils.currentUserHasRole("SUPER_ADMIN")
                || SecurityUtils.currentUserHasRole("DIRECTOR")
                || SecurityUtils.currentUserHasAuthority("CALIFICACIONES_READ_ALL")) {
            return;
        }
        validarDocentePropietario(asignacion);
    }

    private void validarAccesoEscritura(AsignacionDocente asignacion) {
        if (SecurityUtils.currentUserHasRole("ADMIN_INSTITUCION")
                || SecurityUtils.currentUserHasRole("SUPER_ADMIN")
                || SecurityUtils.currentUserHasAuthority("CALIFICACIONES_WRITE")) {
            return;
        }
        validarDocentePropietario(asignacion);
    }

    private void validarDocentePropietario(AsignacionDocente asignacion) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        UUID idUsuario = SecurityUtils.currentUserId();

        Docente docente = docenteRepository.findByIdUsuarioAndIdInstitucion(idUsuario, idInstitucion)
                .orElseThrow(() -> new AccessDeniedException("El usuario autenticado no tiene docente asociado"));

        if (!docente.getId().equals(asignacion.getIdDocente())) {
            throw new AccessDeniedException("No puedes acceder a calificaciones de una asignacion que no te pertenece");
        }
    }

    private void validarEvaluacionEditable(Evaluacion evaluacion) {
        if ("ABIERTA".equals(evaluacion.getEstado())) {
            return;
        }
        if (SecurityUtils.currentUserHasRole("ADMIN_INSTITUCION")
                || SecurityUtils.currentUserHasRole("SUPER_ADMIN")
                || SecurityUtils.currentUserHasAuthority("CALIFICACIONES_OVERRIDE_CIERRE")) {
            return;
        }
        throw new AccessDeniedException("La evaluacion esta cerrada y no puede modificarse");
    }

    private boolean puedeEditarEvaluacion(Evaluacion evaluacion) {
        return "ABIERTA".equals(evaluacion.getEstado())
                || SecurityUtils.currentUserHasRole("ADMIN_INSTITUCION")
                || SecurityUtils.currentUserHasRole("SUPER_ADMIN")
                || SecurityUtils.currentUserHasAuthority("CALIFICACIONES_OVERRIDE_CIERRE");
    }

    private void validarPeriodo(UUID idInstitucion, Integer periodo) {
        if (periodo == null || periodo < 1) {
            throw new IllegalArgumentException("El periodo es obligatorio y debe ser mayor a cero");
        }
        int cantidadPeriodos = configuracionService.getInt(idInstitucion, "CANTIDAD_PERIODOS");
        if (periodo > cantidadPeriodos) {
            throw new IllegalArgumentException("El periodo no puede superar la configuracion institucional: " + cantidadPeriodos);
        }
    }

    private void validarPonderacionTotal(UUID idInstitucion, UUID idAsignacionDocente, Integer periodo,
                                         BigDecimal ponderacionNueva, UUID idExcluir) {
        BigDecimal acumulada = evaluacionRepository.sumPonderacionActiva(idInstitucion, idAsignacionDocente, periodo, idExcluir);
        if (acumulada.add(ponderacionNueva).compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalStateException("La ponderacion total del periodo no puede superar 100%");
        }
    }

    private String normalizarTexto(String value, String mensaje) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(mensaje);
        }
        return value.trim();
    }

    private BigDecimal normalizarPonderacion(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0 || value.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("La ponderacion debe estar entre 0.01 y 100");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizarEscala(String escala) {
        String normalizada = escala == null || escala.isBlank() ? "NUMERICA" : escala.trim().toUpperCase();
        if (!ESCALAS_VALIDAS.contains(normalizada)) {
            throw new IllegalArgumentException("Escala de calificacion invalida: " + escala);
        }
        return normalizada;
    }

    private String normalizarEstadoEvaluacion(String estado) {
        String normalizado = estado == null || estado.isBlank() ? "ABIERTA" : estado.trim().toUpperCase();
        if (!ESTADOS_EVALUACION_VALIDOS.contains(normalizado)) {
            throw new IllegalArgumentException("Estado de evaluacion invalido: " + estado);
        }
        return normalizado;
    }

    private ValorNota normalizarNota(Evaluacion evaluacion, CalificacionDetalleRequest detalle, BigDecimal maximo) {
        if ("NUMERICA".equals(evaluacion.getEscala())) {
            if (detalle.getNotaNumerica() == null) {
                throw new IllegalArgumentException("La nota numerica es obligatoria para la evaluacion seleccionada");
            }
            BigDecimal nota = detalle.getNotaNumerica().setScale(2, RoundingMode.HALF_UP);
            if (nota.compareTo(BigDecimal.ZERO) < 0 || nota.compareTo(maximo) > 0) {
                throw new IllegalArgumentException("La nota numerica debe estar entre 0 y " + maximo);
            }
            return new ValorNota(nota, null);
        }

        String literal = detalle.getNotaLiteral() == null ? "" : detalle.getNotaLiteral().trim().toUpperCase();
        if (!NOTAS_LITERAL_VALIDAS.contains(literal)) {
            throw new IllegalArgumentException("La nota literal debe ser una de: A, B, C, D, F");
        }
        return new ValorNota(null, literal);
    }

    private BigDecimal escalaMaxima(UUID idInstitucion) {
        return BigDecimal.valueOf(configuracionService.getInt(idInstitucion, "ESCALA_CALIFICACION"));
    }

    private List<CalificacionEstudianteResponse> construirEstudiantesResponse(
            List<Inscripcion> inscripciones,
            Map<UUID, Calificacion> calificacionesPorInscripcion
    ) {
        Map<UUID, Estudiante> estudiantesPorId = estudiantesPorId(inscripciones);

        return inscripciones.stream()
                .map(inscripcion -> {
                    Estudiante estudiante = estudiantesPorId.get(inscripcion.getIdEstudiante());
                    if (estudiante == null) {
                        throw new EntityNotFoundException("Estudiante no encontrado: " + inscripcion.getIdEstudiante());
                    }
                    Calificacion calificacion = calificacionesPorInscripcion.get(inscripcion.getId());
                    return CalificacionEstudianteResponse.builder()
                            .idCalificacion(calificacion == null ? null : calificacion.getId())
                            .idInscripcion(inscripcion.getId())
                            .idEstudiante(estudiante.getId())
                            .codigoEstudiante(estudiante.getCodigoEstudiante())
                            .documentoIdentidad(estudiante.getDocumentoIdentidad())
                            .nombres(estudiante.getNombres())
                            .apellidos(estudiante.getApellidos())
                            .nombreCompleto(estudiante.getNombres() + " " + estudiante.getApellidos())
                            .notaNumerica(calificacion == null ? null : calificacion.getNotaNumerica())
                            .notaLiteral(calificacion == null ? null : calificacion.getNotaLiteral())
                            .registrado(calificacion != null)
                            .build();
                })
                .sorted(Comparator.comparing(CalificacionEstudianteResponse::getApellidos)
                        .thenComparing(CalificacionEstudianteResponse::getNombres))
                .toList();
    }

    private Map<UUID, Estudiante> estudiantesPorId(List<Inscripcion> inscripciones) {
        List<UUID> idsEstudiantes = inscripciones.stream()
                .map(Inscripcion::getIdEstudiante)
                .distinct()
                .toList();
        return estudianteRepository.findAllById(idsEstudiantes)
                .stream()
                .collect(Collectors.toMap(Estudiante::getId, Function.identity()));
    }

    private CalificacionAsignacionResponse toAsignacionResponse(AsignacionDocente asignacion) {
        UUID idInstitucion = asignacion.getIdInstitucion();

        Docente docente = docenteRepository.findByIdAndIdInstitucion(asignacion.getIdDocente(), idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Docente no encontrado: " + asignacion.getIdDocente()));
        Materia materia = materiaRepository.findByIdAndIdInstitucion(asignacion.getIdMateria(), idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Materia no encontrada: " + asignacion.getIdMateria()));
        Paralelo paralelo = paraleloRepository.findByIdAndIdInstitucion(asignacion.getIdParalelo(), idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Paralelo no encontrado: " + asignacion.getIdParalelo()));
        Curso curso = cursoRepository.findByIdAndIdInstitucion(paralelo.getIdCurso(), idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Curso no encontrado: " + paralelo.getIdCurso()));
        GestionAcademica gestion = gestionAcademicaRepository.findByIdAndIdInstitucion(asignacion.getIdGestion(), idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Gestion academica no encontrada: " + asignacion.getIdGestion()));

        return CalificacionAsignacionResponse.builder()
                .idAsignacionDocente(asignacion.getId())
                .idDocente(docente.getId())
                .codigoDocente(docente.getCodigo())
                .nombreDocente(docente.getNombres() + " " + docente.getApellidos())
                .idMateria(materia.getId())
                .codigoMateria(materia.getCodigo())
                .nombreMateria(materia.getNombre())
                .idParalelo(paralelo.getId())
                .nombreParalelo(paralelo.getNombre())
                .idCurso(curso.getId())
                .nombreCurso(curso.getNombre())
                .idGestion(gestion.getId())
                .nombreGestion(gestion.getNombre())
                .estado(asignacion.getEstado())
                .build();
    }

    private record ValorNota(BigDecimal notaNumerica, String notaLiteral) {
        static ValorNota from(Calificacion calificacion) {
            return new ValorNota(calificacion.getNotaNumerica(), calificacion.getNotaLiteral());
        }

        String valorTexto() {
            return notaNumerica != null ? notaNumerica.toPlainString() : notaLiteral;
        }
    }
}
