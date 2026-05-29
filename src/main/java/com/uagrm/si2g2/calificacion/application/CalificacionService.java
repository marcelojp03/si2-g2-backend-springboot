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

/**
 * ====================================================================
 * SERVICIO: CalificacionService
 * ====================================================================
 * 
 * Lógica de negocio completa para gestión de evaluaciones y calificaciones.
 * Responsabilidades:
 * 
 * 1. EVALUACIONES:
 * - Crear evaluaciones (parcial, examen, trabajo, proyecto, participación)
 * - Actualizar evaluaciones (cambiar nombre, tipo, ponderación, estado)
 * - Validar ponderación total no supere 100%
 * - Listar evaluaciones por materia/período
 * 
 * 2. CALIFICACIONES:
 * - Registrar notas nuevas (numéricas o literales)
 * - Actualizar notas existentes con auditoría de cambios
 * - Generar plantilla de calificaciones para docente
 * - Calcular notas consolidadas y estado académico
 * 
 * 3. SEGURIDAD:
 * - Validación de acceso: docente solo ve sus materias asignadas
 * - Administradores pueden ver todas las materias
 * - Restricción de edición cuando evaluación está CERRADA
 * - Auditoría completa de cambios en CalificacionCambio
 * 
 * 4. VALIDACIONES:
 * - Escalas válidas: NUMERICA, LITERAL
 * - Notas literales: A, B, C, D, F
 * - Ponderación: 0.01 a 100%
 * - Períodos: 1 a 6 (configurable por institución)
 * - Solo inscripciones ACTIVAS
 * - Evaluaciones ABIERTA (o override si es admin)
 * 
 * PATRONES USADOS:
 * - Transaccional: @Transactional para CRUD, @Transactional(readOnly=true) para
 * consultas
 * - Validación temprana: Lanza excepciones antes de queries
 * - DTO separado: Respuestas estandarizadas no exponen entidades
 * - Record ValorNota: Encapsula conversión numerica/literal
 */
@Service
@RequiredArgsConstructor
public class CalificacionService {

    // Constantes de validación
    private static final String ESTADO_ASIGNACION_ACTIVA = "ACTIVA";
    private static final String ESTADO_INSCRIPCION_ACTIVA = "ACTIVA";
    private static final Set<String> ESCALAS_VALIDAS = Set.of("NUMERICA", "LITERAL");
    private static final Set<String> ESTADOS_EVALUACION_VALIDOS = Set.of("ABIERTA", "CERRADA", "ANULADA");
    private static final Set<String> NOTAS_LITERAL_VALIDAS = Set.of("A", "B", "C", "D", "F");

    // Repositories para acceso a datos
    private final EvaluacionRepository evaluacionRepository;
    private final CalificacionRepository calificacionRepository;
    private final CalificacionCambioRepository cambioRepository;

    // Repositorios relacionados (para obtener datos de docentes, estudiantes, etc.)
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

    /**
     * OPERACIÓN: Listar asignaciones donde se pueden registrar calificaciones
     * 
     * LÓGICA:
     * 1. Obtiene id_institucion del usuario autenticado (multitenencia)
     * 2. Si es DOCENTE: filtra solo SUS asignaciones
     * 3. Si es ADMIN/DIRECTOR/SUPER_ADMIN: lista todas
     * 4. Filtra solo asignaciones en estado ACTIVA
     * 
     * CASOS DE USO:
     * - Cargar el punto de entrada del módulo en la UI
     * - Seleccionar la materia a la que pertenecen las evaluaciones
     * - Verificar a qué paralelos tiene acceso el docente
     * 
     * @return Lista de asignaciones con info de materia, curso, paralelo, docente
     */
    @Transactional(readOnly = true)
    public List<CalificacionAsignacionResponse> listarMisAsignaciones() {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();

        List<AsignacionDocente> asignaciones;
        if (SecurityUtils.currentUserHasRole("DOCENTE")) {
            // DOCENTE: obtiene su perfil de docente y filtra sus asignaciones
            Docente docente = docenteRepository
                    .findByIdUsuarioAndIdInstitucion(SecurityUtils.currentUserId(), idInstitucion)
                    .orElseThrow(
                            () -> new EntityNotFoundException("No existe un docente asociado al usuario autenticado"));

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
    public List<EvaluacionResponse> listarEvaluacionesPorMateria(UUID idMateria, Integer periodo) {
        // La materia es el nuevo eje de negocio: todos los paralelos comparten
        // la misma estructura de evaluaciones para el mismo período.
        // 1. Se obtiene la institución del usuario autenticado para mantener
        // aislamiento multi-institución
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();

        // 2. Validar que la materia existe y pertenece a la institución
        if (!materiaRepository.existsByIdAndIdInstitucion(idMateria, idInstitucion)) {
            throw new EntityNotFoundException("Materia no encontrada");
        }

        // 3. Verificar acceso: Un docente solo puede ver evaluaciones de materias que
        // enseña.
        // Un admin/director puede ver todas.
        validarAccesoLecturaMateria(idMateria, idInstitucion);

        // 4. Si el período llega null, se listan todas las evaluaciones de la materia.
        // Si llega un período, se filtra solo ese período académico.
        List<Evaluacion> evaluaciones = periodo == null
                ? evaluacionRepository.findAllByIdInstitucionAndIdMateria(idInstitucion, idMateria)
                : evaluacionRepository.findAllByIdInstitucionAndIdMateriaAndPeriodo(idInstitucion, idMateria, periodo);

        // 5. Las entidades se ordenan y se convierten a DTO
        return evaluaciones.stream()
                .sorted(Comparator.comparing(Evaluacion::getPeriodo)
                        .thenComparing(Evaluacion::getNombre))
                .map(EvaluacionResponse::from)
                .toList();
    }

    @Deprecated(forRemoval = true)
    @Transactional(readOnly = true)
    public List<EvaluacionResponse> listarEvaluaciones(UUID idAsignacionDocente, Integer periodo) {
        // Compatibilidad temporal: este método conserva la firma antigua, pero
        // resuelve las evaluaciones por materia para no romper integraciones viejas.
        // 1. Se obtiene la institucion del usuario autenticado para mantener
        // aislamiento multi-institucion: ningun usuario consulta datos de otra
        // institucion.
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();

        // 2. Se valida que la asignacion exista, pertenezca a la institucion actual
        // y este activa. Si no cumple, se corta el flujo con excepcion.
        AsignacionDocente asignacion = buscarAsignacionActiva(idAsignacionDocente, idInstitucion);

        // 3. Se verifica si el usuario puede leer esta asignacion.
        // Un docente solo puede leer sus propias asignaciones; un admin/director
        // puede consultar todas las de su institucion.
        validarAccesoLectura(asignacion);

        // 4. Si el periodo llega null, se listan todas las evaluaciones de la
        // asignacion.
        // Si llega un periodo, se filtra solo ese periodo academico.
        List<Evaluacion> evaluaciones = periodo == null
                ? evaluacionRepository.findAllByIdInstitucionAndIdMateria(idInstitucion, asignacion.getIdMateria())
                : evaluacionRepository.findAllByIdInstitucionAndIdMateriaAndPeriodo(idInstitucion,
                        asignacion.getIdMateria(), periodo);

        // 5. Las entidades se ordenan y se convierten a DTO para no exponer
        // directamente la estructura interna de la base de datos.
        return evaluaciones.stream()
                .sorted(Comparator.comparing(Evaluacion::getPeriodo)
                        .thenComparing(Evaluacion::getNombre))
                .map(EvaluacionResponse::from)
                .toList();
    }

    @Transactional
    public EvaluacionResponse crearEvaluacion(EvaluacionRequest request) {
        // 1. Contexto base: institución actual
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();

        // 2. Validar que la materia existe. Ya no se guarda una evaluación por
        // asignación docente, sino por materia.
        if (!materiaRepository.existsByIdAndIdInstitucion(request.getIdMateria(), idInstitucion)) {
            throw new EntityNotFoundException("Materia no encontrada");
        }

        // 3. Validar permisos: Solo docentes de esta materia o administradores pueden
        // crear evaluaciones
        validarAccesoEscrituraMateria(request.getIdMateria(), idInstitucion);

        // 4. El periodo debe existir según la configuración institucional.
        // Ejemplo: si la institución tiene 4 períodos, no permite período 5.
        validarPeriodo(idInstitucion, request.getPeriodo());

        // 5. Se limpian y validan los datos recibidos antes de guardar:
        // nombre/tipo obligatorios, escala válida y ponderación entre 0.01 y 100.
        String nombre = normalizarTexto(request.getNombre(), "El nombre de la evaluación es obligatorio");
        String tipo = normalizarTexto(request.getTipo(), "El tipo de evaluación es obligatorio");
        String escala = normalizarEscala(request.getEscala());
        BigDecimal ponderacion = normalizarPonderacion(request.getPonderacion());

        // 6. Regla de negocio: en una misma materia y período no puede haber
        // dos evaluaciones con el mismo nombre.
        if (evaluacionRepository.existsByIdInstitucionAndIdMateriaAndPeriodoAndNombreIgnoreCase(
                idInstitucion, request.getIdMateria(), request.getPeriodo(), nombre)) {
            throw new IllegalStateException("Ya existe una evaluación con ese nombre en el período seleccionado");
        }

        // 7. Regla de negocio: la suma de ponderaciones activas del período
        // no puede superar el 100%.
        validarPonderacionTotal(idInstitucion, request.getIdMateria(), request.getPeriodo(), ponderacion, null);

        // 8. Se arma la entidad Evaluación. Aunque el request traiga otro estado,
        // al crear siempre inicia en ABIERTA para permitir registrar notas.
        Evaluacion evaluacion = Evaluacion.builder()
                .idInstitucion(idInstitucion)
                .idMateria(request.getIdMateria())
                .creadoPor(SecurityUtils.currentUserId())
                .periodo(request.getPeriodo())
                .tipo(tipo)
                .nombre(nombre)
                .ponderacion(ponderacion)
                .escala(escala)
                .estado("ABIERTA")
                .build();

        // 9. Se persiste en base de datos y luego se registra auditoría general
        // para dejar trazabilidad de quién creó la evaluación.
        Evaluacion saved = evaluacionRepository.save(evaluacion);
        auditoriaService.registrar(idInstitucion, SecurityUtils.currentUserId(), "CALIFICACIONES",
                "CREAR_EVALUACION", "evaluacion", saved.getId().toString(), true,
                "Evaluación creada");

        // 10. Se devuelve un DTO de respuesta para el frontend.
        return EvaluacionResponse.from(saved);
    }

    @Transactional
    public EvaluacionResponse actualizarEvaluacion(UUID id, EvaluacionRequest request) {
        // 1. Se busca la evaluacion dentro de la institucion actual.
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        Evaluacion evaluacion = buscarEvaluacion(id, idInstitucion);

        // 2. Se valida acceso: docente solo puede editar materias que enseña.
        validarAccesoEscrituraMateria(evaluacion.getIdMateria(), idInstitucion);

        // 3. Se guarda una foto de los datos anteriores para auditoria detallada.
        // Esto permite comparar "antes" y "despues".
        Map<String, Object> antes = Map.of(
                "tipo", evaluacion.getTipo(),
                "nombre", evaluacion.getNombre(),
                "ponderacion", evaluacion.getPonderacion(),
                "escala", evaluacion.getEscala(),
                "estado", evaluacion.getEstado());

        // 4. Se validan y normalizan los nuevos valores recibidos desde el frontend.
        String nombre = normalizarTexto(request.getNombre(), "El nombre de la evaluacion es obligatorio");
        String tipo = normalizarTexto(request.getTipo(), "El tipo de evaluacion es obligatorio");
        String escala = normalizarEscala(request.getEscala());
        String estado = normalizarEstadoEvaluacion(request.getEstado());
        BigDecimal ponderacion = normalizarPonderacion(request.getPonderacion());

        // 5. Se valida la ponderacion excluyendo la evaluacion actual.
        // Asi se puede editar una evaluacion sin contarse dos veces en la suma.
        validarPonderacionTotal(idInstitucion, evaluacion.getIdMateria(), evaluacion.getPeriodo(), ponderacion,
                evaluacion.getId());

        // 6. Se aplican los nuevos valores a la entidad existente.
        evaluacion.setTipo(tipo);
        evaluacion.setNombre(nombre);
        evaluacion.setPonderacion(ponderacion);
        evaluacion.setEscala(escala);
        evaluacion.setEstado(estado);

        // 7. Se guarda y se registra auditoria detallada con valores anteriores y
        // nuevos.
        Evaluacion saved = evaluacionRepository.save(evaluacion);
        auditoriaService.registrarDetallado(idInstitucion, SecurityUtils.currentUserId(), "CALIFICACIONES",
                "ACTUALIZAR_EVALUACION", "evaluacion", saved.getId().toString(), antes,
                Map.of("tipo", saved.getTipo(), "nombre", saved.getNombre(), "ponderacion", saved.getPonderacion(),
                        "escala", saved.getEscala(), "estado", saved.getEstado()),
                true, "Evaluacion actualizada");

        // 8. Se devuelve la evaluacion actualizada como DTO.
        return EvaluacionResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public CalificacionPlantillaResponse obtenerPlantilla(UUID idEvaluacion) {
        // 1. Se identifica la institucion y se busca la evaluacion seleccionada.
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        Evaluacion evaluacion = buscarEvaluacion(idEvaluacion, idInstitucion);

        // 2. Validar acceso a la materia de esta evaluacion.
        validarAccesoLecturaMateria(evaluacion.getIdMateria(), idInstitucion);

        // 3. Se busca la materia para contexto completo.
        Materia materia = materiaRepository.findByIdAndIdInstitucion(evaluacion.getIdMateria(), idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Materia no encontrada: " + evaluacion.getIdMateria()));

        // 4. Se necesitan las asignaciones de esta materia para obtener estudiantes.
        // Aunque la evaluación ya no depende de la asignación, las inscripciones
        // siguen viviendo por paralelo, así que se toma una asignación activa para
        // ubicar ese grupo.
        List<AsignacionDocente> asignacionesDeMateria = asignacionDocenteRepository
                .findByIdMateriaAndIdInstitucionAndEstado(evaluacion.getIdMateria(), idInstitucion,
                        ESTADO_ASIGNACION_ACTIVA);
        if (asignacionesDeMateria.isEmpty()) {
            throw new EntityNotFoundException("No hay asignaciones activas para esta materia");
        }
        AsignacionDocente asignacion = asignacionesDeMateria.get(0);

        // 5. Se cargan solo estudiantes con inscripcion ACTIVA para ese paralelo y
        // gestion.
        List<Inscripcion> inscripciones = obtenerInscripcionesActivasDeAsignacion(idInstitucion, asignacion);

        // 6. Se consultan las notas ya registradas de esta evaluacion y se convierten
        // en un mapa por idInscripcion para encontrar rapidamente la nota de cada
        // estudiante.
        Map<UUID, Calificacion> calificacionesPorInscripcion = calificacionRepository
                .findAllByIdEvaluacion(evaluacion.getId())
                .stream()
                .collect(Collectors.toMap(Calificacion::getIdInscripcion, Function.identity()));

        // 7. Se une informacion de inscripcion + estudiante + calificacion existente.
        List<CalificacionEstudianteResponse> estudiantes = construirEstudiantesResponse(inscripciones,
                calificacionesPorInscripcion);

        // 8. Se arma la respuesta que consume Angular para dibujar la tabla de notas.
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
        // 1. Se valida el contexto: institucion, evaluacion y materia.
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        Evaluacion evaluacion = buscarEvaluacion(request.getIdEvaluacion(), idInstitucion);

        // 2. Se valida permiso de escritura y que la evaluacion pueda modificarse.
        // Si esta CERRADA, solo un usuario con permiso especial puede editarla.
        validarAccesoEscrituraMateria(evaluacion.getIdMateria(), idInstitucion);
        validarEvaluacionEditable(evaluacion);

        // 3. Se busca una asignacion de la materia para obtener el paralelo/gestion.
        // Se toma una asignación activa solo para localizar el grupo de inscripciones.
        List<AsignacionDocente> asignacionesDeMateria = asignacionDocenteRepository
                .findByIdMateriaAndIdInstitucionAndEstado(evaluacion.getIdMateria(), idInstitucion,
                        ESTADO_ASIGNACION_ACTIVA);
        if (asignacionesDeMateria.isEmpty()) {
            throw new EntityNotFoundException("No hay asignaciones activas para esta materia");
        }
        AsignacionDocente asignacion = asignacionesDeMateria.get(0);

        // 4. Se obtienen las inscripciones validas para impedir que el frontend
        // envie notas de estudiantes que no pertenecen al paralelo.
        List<Inscripcion> inscripciones = obtenerInscripcionesActivasDeAsignacion(idInstitucion, asignacion);
        Set<UUID> idsInscripcionesValidas = inscripciones.stream().map(Inscripcion::getId).collect(Collectors.toSet());
        Set<UUID> idsRecibidos = new HashSet<>();

        // 5. Validacion previa de todos los detalles:
        // - cada fila debe traer idInscripcion
        // - no se permiten inscripciones duplicadas
        // - todas deben pertenecer a la asignacion/evaluacion actual
        for (CalificacionDetalleRequest detalle : request.getDetalles()) {
            if (detalle.getIdInscripcion() == null) {
                throw new IllegalArgumentException("La inscripcion es obligatoria para cada calificacion");
            }
            if (!idsRecibidos.add(detalle.getIdInscripcion())) {
                throw new IllegalArgumentException("La inscripcion esta duplicada en la carga de calificaciones");
            }
            if (!idsInscripcionesValidas.contains(detalle.getIdInscripcion())) {
                throw new IllegalArgumentException(
                        "La inscripcion no pertenece al paralelo y gestion de la evaluacion");
            }
        }

        // 6. Escala maxima institucional. Normalmente es 100, pero se toma
        // de configuracion para respetar reglas de la institucion.
        BigDecimal maximo = escalaMaxima(idInstitucion);
        List<Calificacion> guardadas = new ArrayList<>();

        // 7. Se procesa cada nota enviada por el frontend.
        for (CalificacionDetalleRequest detalle : request.getDetalles()) {
            // 6.1. Normaliza la nota segun la escala de la evaluacion:
            // si es NUMERICA exige notaNumerica; si es LITERAL exige A/B/C/D/F.
            ValorNota valorNuevo = normalizarNota(evaluacion, detalle, maximo);

            // 6.2. Busca si ya existe una calificacion para esa evaluacion + inscripcion.
            // Si no existe, se crea; si existe, se compara para decidir si se actualiza.
            Calificacion calificacion = calificacionRepository
                    .findByIdEvaluacionAndIdInscripcion(evaluacion.getId(), detalle.getIdInscripcion())
                    .orElse(null);

            if (calificacion == null) {
                // 6.3. Caso nuevo: se registra por primera vez la nota del estudiante.
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
                // 6.4. Caso existente: se compara valor anterior vs nuevo.
                // Si no cambio, no se guarda ni se audita para evitar registros innecesarios.
                ValorNota valorAnterior = ValorNota.from(calificacion);
                if (!valorAnterior.valorTexto().equals(valorNuevo.valorTexto())) {
                    // 6.5. Si cambia una nota ya registrada, la razon es obligatoria
                    // porque se necesita trazabilidad academica.
                    if (detalle.getRazonCambio() == null || detalle.getRazonCambio().isBlank()) {
                        throw new IllegalArgumentException(
                                "La razon de cambio es obligatoria al modificar una calificacion");
                    }

                    // 6.6. Se actualiza la nota y el usuario que realizo el cambio.
                    calificacion.setNotaNumerica(valorNuevo.notaNumerica());
                    calificacion.setNotaLiteral(valorNuevo.notaLiteral());
                    calificacion.setRegistradoPor(SecurityUtils.currentUserId());
                    Calificacion saved = calificacionRepository.save(calificacion);
                    guardadas.add(saved);

                    // 6.7. Auditoria especifica de nota: guarda valor anterior,
                    // valor nuevo, usuario, fecha y razon del cambio.
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

        // 8. Auditoria general de la operacion completa.
        auditoriaService.registrar(idInstitucion, SecurityUtils.currentUserId(), "CALIFICACIONES",
                "GUARDAR_NOTAS", "evaluacion", evaluacion.getId().toString(), true,
                "Calificaciones guardadas: " + guardadas.size());

        // 9. Se devuelve la plantilla recargada para que el frontend vea los datos
        // actualizados.
        return obtenerPlantilla(evaluacion.getId());
    }

    @Transactional(readOnly = true)
    public CalificacionResumenResponse obtenerResumen(UUID idAsignacionDocente, Integer periodo) {
        // 1. Se valida institucion, asignacion, permisos y periodo.
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        AsignacionDocente asignacion = buscarAsignacionActiva(idAsignacionDocente, idInstitucion);
        validarAccesoLectura(asignacion);
        validarPeriodo(idInstitucion, periodo);

        // 2. Se toman solo evaluaciones del periodo que no esten ANULADAS.
        // Nota: ahora la consulta principal es por idMateria; idAsignacionDocente
        // se conserva solo como entrada del resumen para derivar la materia.
        List<Evaluacion> evaluaciones = evaluacionRepository
                .findAllByIdInstitucionAndIdMateriaAndPeriodo(idInstitucion, asignacion.getIdMateria(), periodo)
                .stream()
                .filter(e -> !"ANULADA".equals(e.getEstado()))
                .toList();

        // 3. Se preparan estructuras para buscar evaluaciones por id y consultar
        // todas las calificaciones del periodo en una sola consulta.
        List<UUID> idsEvaluaciones = evaluaciones.stream().map(Evaluacion::getId).toList();
        Map<UUID, Evaluacion> evaluacionesPorId = evaluaciones.stream()
                .collect(Collectors.toMap(Evaluacion::getId, Function.identity()));

        List<Calificacion> calificaciones = idsEvaluaciones.isEmpty()
                ? List.of()
                : calificacionRepository.findAllByIdEvaluacionIn(idsEvaluaciones);

        // 4. Se agrupan las notas por inscripcion para calcular el resumen
        // estudiante por estudiante.
        Map<UUID, List<Calificacion>> calificacionesPorInscripcion = calificaciones.stream()
                .collect(Collectors.groupingBy(Calificacion::getIdInscripcion));

        // 5. Se cargan estudiantes activos y la nota minima de aprobacion
        // configurada para la institucion.
        List<Inscripcion> inscripciones = obtenerInscripcionesActivasDeAsignacion(idInstitucion, asignacion);
        Map<UUID, Estudiante> estudiantes = estudiantesPorId(inscripciones);
        BigDecimal notaMinima = BigDecimal
                .valueOf(configuracionService.getInt(idInstitucion, "NOTA_MINIMA_APROBACION"));

        // 6. Para cada estudiante se calcula:
        // nota consolidada, ponderacion registrada y estado academico.
        List<CalificacionResumenEstudianteResponse> resumenEstudiantes = inscripciones.stream()
                .map(inscripcion -> construirResumenEstudiante(
                        inscripcion,
                        estudiantes.get(inscripcion.getIdEstudiante()),
                        calificacionesPorInscripcion.getOrDefault(inscripcion.getId(), List.of()),
                        evaluacionesPorId,
                        notaMinima))
                .sorted(Comparator.comparing(CalificacionResumenEstudianteResponse::getNombreCompleto))
                .toList();

        // 7. Suma de ponderaciones configuradas en el periodo.
        BigDecimal ponderacionTotal = evaluaciones.stream()
                .map(Evaluacion::getPonderacion)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 8. Respuesta final para la tabla de resumen del frontend.
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
            BigDecimal notaMinima) {
        // Valida que la inscripcion apunte a un estudiante existente.
        if (estudiante == null) {
            throw new EntityNotFoundException("Estudiante no encontrado: " + inscripcion.getIdEstudiante());
        }

        BigDecimal acumulado = BigDecimal.ZERO;
        BigDecimal ponderacionRegistrada = BigDecimal.ZERO;

        // Recorre las calificaciones del estudiante en el periodo.
        // Solo las evaluaciones NUMERICAS participan en el promedio consolidado.
        for (Calificacion calificacion : calificaciones) {
            Evaluacion evaluacion = evaluacionesPorId.get(calificacion.getIdEvaluacion());
            if (evaluacion == null || calificacion.getNotaNumerica() == null
                    || !"NUMERICA".equals(evaluacion.getEscala())) {
                continue;
            }
            // Formula: nota parcial ponderada = nota * ponderacion / 100.
            acumulado = acumulado.add(calificacion.getNotaNumerica()
                    .multiply(evaluacion.getPonderacion())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            ponderacionRegistrada = ponderacionRegistrada.add(evaluacion.getPonderacion());
        }

        // Si la nota acumulada alcanza la minima institucional, queda APROBADO;
        // caso contrario queda EN_RIESGO.
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
        // Busca por id + institucion para evitar que un usuario acceda a registros
        // de otra institucion aunque conozca el UUID.
        return evaluacionRepository.findByIdAndIdInstitucion(idEvaluacion, idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Evaluacion no encontrada: " + idEvaluacion));
    }

    private AsignacionDocente buscarAsignacionActiva(UUID idAsignacionDocente, UUID idInstitucion) {
        // Centraliza la busqueda de asignacion y la validacion de estado ACTIVA.
        AsignacionDocente asignacion = asignacionDocenteRepository
                .findByIdAndIdInstitucion(idAsignacionDocente, idInstitucion)
                .orElseThrow(
                        () -> new EntityNotFoundException("Asignacion docente no encontrada: " + idAsignacionDocente));
        if (!ESTADO_ASIGNACION_ACTIVA.equals(asignacion.getEstado())) {
            throw new IllegalStateException("La asignacion docente no esta activa");
        }
        return asignacion;
    }

    private List<Inscripcion> obtenerInscripcionesActivasDeAsignacion(UUID idInstitucion,
            AsignacionDocente asignacion) {
        // Obtiene estudiantes inscritos en el paralelo de la asignacion, pero
        // solo si pertenecen a la misma gestion academica y siguen ACTIVOS.
        return inscripcionRepository
                .findAllByIdInstitucionAndIdParalelo(idInstitucion, asignacion.getIdParalelo())
                .stream()
                .filter(i -> asignacion.getIdGestion().equals(i.getIdGestion()))
                .filter(i -> ESTADO_INSCRIPCION_ACTIVA.equals(i.getEstado()))
                .sorted(Comparator.comparing(i -> i.getId().toString()))
                .toList();
    }

    private void validarAccesoLectura(AsignacionDocente asignacion) {
        // Roles administrativos o permiso global pueden leer todas las asignaciones.
        if (SecurityUtils.currentUserHasRole("ADMIN_INSTITUCION")
                || SecurityUtils.currentUserHasRole("SUPER_ADMIN")
                || SecurityUtils.currentUserHasRole("DIRECTOR")
                || SecurityUtils.currentUserHasAuthority("CALIFICACIONES_READ_ALL")) {
            return;
        }
        // Si no es administrativo, debe ser el docente propietario.
        validarDocentePropietario(asignacion);
    }

    private void validarAccesoEscritura(AsignacionDocente asignacion) {
        // Escritura global: administradores o usuarios con permiso
        // CALIFICACIONES_WRITE.
        if (SecurityUtils.currentUserHasRole("ADMIN_INSTITUCION")
                || SecurityUtils.currentUserHasRole("SUPER_ADMIN")
                || SecurityUtils.currentUserHasAuthority("CALIFICACIONES_WRITE")) {
            return;
        }
        // Si no tiene escritura global, solo puede escribir el docente dueno.
        validarDocentePropietario(asignacion);
    }

    private void validarDocentePropietario(AsignacionDocente asignacion) {
        // Vincula el usuario autenticado con su entidad Docente.
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        UUID idUsuario = SecurityUtils.currentUserId();

        Docente docente = docenteRepository.findByIdUsuarioAndIdInstitucion(idUsuario, idInstitucion)
                .orElseThrow(() -> new AccessDeniedException("El usuario autenticado no tiene docente asociado"));

        // La asignacion solo es accesible si pertenece a ese docente.
        if (!docente.getId().equals(asignacion.getIdDocente())) {
            throw new AccessDeniedException("No puedes acceder a calificaciones de una asignacion que no te pertenece");
        }
    }

    private void validarAccesoLecturaMateria(UUID idMateria, UUID idInstitucion) {
        // Roles administrativos o permiso global pueden leer todas las materias.
        if (SecurityUtils.currentUserHasRole("ADMIN_INSTITUCION")
                || SecurityUtils.currentUserHasRole("SUPER_ADMIN")
                || SecurityUtils.currentUserHasRole("DIRECTOR")
                || SecurityUtils.currentUserHasAuthority("CALIFICACIONES_READ_ALL")) {
            return;
        }
        // Si no es administrativo, debe tener al menos una asignación en esa materia.
        validarDocenteTieneMateria(idMateria, idInstitucion);
    }

    private void validarAccesoEscrituraMateria(UUID idMateria, UUID idInstitucion) {
        // Escritura global: administradores o usuarios con permiso
        // CALIFICACIONES_WRITE.
        if (SecurityUtils.currentUserHasRole("ADMIN_INSTITUCION")
                || SecurityUtils.currentUserHasRole("SUPER_ADMIN")
                || SecurityUtils.currentUserHasAuthority("CALIFICACIONES_WRITE")) {
            return;
        }
        // Si no tiene escritura global, debe tener asignación en esa materia.
        validarDocenteTieneMateria(idMateria, idInstitucion);
    }

    private void validarDocenteTieneMateria(UUID idMateria, UUID idInstitucion) {
        // Verificar que el docente autenticado tiene al menos una asignación en esta
        // materia.
        UUID idUsuario = SecurityUtils.currentUserId();
        Docente docente = docenteRepository.findByIdUsuarioAndIdInstitucion(idUsuario, idInstitucion)
                .orElseThrow(() -> new AccessDeniedException("El usuario autenticado no tiene docente asociado"));

        // Verificar que existe al menos una asignación activa del docente para esta
        // materia.
        boolean tieneMateria = asignacionDocenteRepository
                .existsByIdDocenteAndIdMateriaAndEstado(docente.getId(), idMateria, ESTADO_ASIGNACION_ACTIVA);

        if (!tieneMateria) {
            throw new AccessDeniedException(
                    "No tienes asignación como docente para esta materia");
        }
    }

    private void validarEvaluacionEditable(Evaluacion evaluacion) {
        // Caso normal: una evaluacion ABIERTA puede modificarse.
        if ("ABIERTA".equals(evaluacion.getEstado())) {
            return;
        }
        // Caso especial: administradores o permiso de override pueden modificar
        // evaluaciones cerradas.
        if (SecurityUtils.currentUserHasRole("ADMIN_INSTITUCION")
                || SecurityUtils.currentUserHasRole("SUPER_ADMIN")
                || SecurityUtils.currentUserHasAuthority("CALIFICACIONES_OVERRIDE_CIERRE")) {
            return;
        }
        throw new AccessDeniedException("La evaluacion esta cerrada y no puede modificarse");
    }

    private boolean puedeEditarEvaluacion(Evaluacion evaluacion) {
        // Se usa para informar al frontend si debe habilitar o bloquear inputs.
        return "ABIERTA".equals(evaluacion.getEstado())
                || SecurityUtils.currentUserHasRole("ADMIN_INSTITUCION")
                || SecurityUtils.currentUserHasRole("SUPER_ADMIN")
                || SecurityUtils.currentUserHasAuthority("CALIFICACIONES_OVERRIDE_CIERRE");
    }

    private void validarPeriodo(UUID idInstitucion, Integer periodo) {
        // El periodo es obligatorio y debe ser positivo.
        if (periodo == null || periodo < 1) {
            throw new IllegalArgumentException("El periodo es obligatorio y debe ser mayor a cero");
        }
        // Se respeta la cantidad de periodos definida en configuracion institucional.
        int cantidadPeriodos = configuracionService.getInt(idInstitucion, "CANTIDAD_PERIODOS");
        if (periodo > cantidadPeriodos) {
            throw new IllegalArgumentException(
                    "El periodo no puede superar la configuracion institucional: " + cantidadPeriodos);
        }
    }

    private void validarPonderacionTotal(UUID idInstitucion, UUID idMateria, Integer periodo,
            BigDecimal ponderacionNueva, UUID idExcluir) {
        // Suma la ponderación de evaluaciones activas del período.
        // idExcluir se usa al editar para no contar dos veces la misma evaluación.
        BigDecimal acumulada = evaluacionRepository.sumPonderacionActiva(idInstitucion, idMateria, periodo,
                idExcluir);

        // Verifica si ya se alcanzó el 100% en el período
        if (acumulada.compareTo(BigDecimal.valueOf(100)) >= 0) {
            throw new IllegalStateException(
                    "Ya se ha utilizado el 100% de la ponderación en este período. " +
                            "Para agregar más evaluaciones, debes editar una existente y reducir su ponderación.");
        }

        // Verifica que la suma no supere el 100%
        BigDecimal suma = acumulada.add(ponderacionNueva);
        if (suma.compareTo(BigDecimal.valueOf(100)) > 0) {
            BigDecimal disponible = BigDecimal.valueOf(100).subtract(acumulada).setScale(2, RoundingMode.DOWN);
            throw new IllegalStateException(
                    "La ponderación ingresada supera el espacio disponible. " +
                            "Espacio disponible para este período: " + disponible + "%");
        }
    }

    private String normalizarTexto(String value, String mensaje) {
        // Valida campo obligatorio y elimina espacios iniciales/finales.
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(mensaje);
        }
        return value.trim();
    }

    private BigDecimal normalizarPonderacion(BigDecimal value) {
        // La ponderacion representa porcentaje. Por eso debe estar entre 0.01 y 100.
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0 || value.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("La ponderacion debe estar entre 0.01 y 100");
        }
        // Se redondea a 2 decimales para mantener consistencia en base de datos.
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizarEscala(String escala) {
        // Si no llega escala, se asume NUMERICA por defecto.
        String normalizada = escala == null || escala.isBlank() ? "NUMERICA" : escala.trim().toUpperCase();
        if (!ESCALAS_VALIDAS.contains(normalizada)) {
            throw new IllegalArgumentException("Escala de calificacion invalida: " + escala);
        }
        return normalizada;
    }

    private String normalizarEstadoEvaluacion(String estado) {
        // Si no llega estado, se asume ABIERTA por defecto.
        String normalizado = estado == null || estado.isBlank() ? "ABIERTA" : estado.trim().toUpperCase();
        if (!ESTADOS_EVALUACION_VALIDOS.contains(normalizado)) {
            throw new IllegalArgumentException("Estado de evaluacion invalido: " + estado);
        }
        return normalizado;
    }

    private ValorNota normalizarNota(Evaluacion evaluacion, CalificacionDetalleRequest detalle, BigDecimal maximo) {
        // Para evaluaciones numericas se exige notaNumerica y se valida contra la
        // escala maxima.
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

        // Para evaluaciones literales se exige una letra valida.
        String literal = detalle.getNotaLiteral() == null ? "" : detalle.getNotaLiteral().trim().toUpperCase();
        if (!NOTAS_LITERAL_VALIDAS.contains(literal)) {
            throw new IllegalArgumentException("La nota literal debe ser una de: A, B, C, D, F");
        }
        return new ValorNota(null, literal);
    }

    private BigDecimal escalaMaxima(UUID idInstitucion) {
        // Lee la escala maxima desde configuracion institucional.
        // Ejemplo usual: 100.
        return BigDecimal.valueOf(configuracionService.getInt(idInstitucion, "ESCALA_CALIFICACION"));
    }

    private List<CalificacionEstudianteResponse> construirEstudiantesResponse(
            List<Inscripcion> inscripciones,
            Map<UUID, Calificacion> calificacionesPorInscripcion) {
        // Carga los estudiantes de las inscripciones para construir filas completas
        // de la plantilla que se envia al frontend.
        Map<UUID, Estudiante> estudiantesPorId = estudiantesPorId(inscripciones);

        return inscripciones.stream()
                .map(inscripcion -> {
                    Estudiante estudiante = estudiantesPorId.get(inscripcion.getIdEstudiante());
                    if (estudiante == null) {
                        throw new EntityNotFoundException("Estudiante no encontrado: " + inscripcion.getIdEstudiante());
                    }
                    Calificacion calificacion = calificacionesPorInscripcion.get(inscripcion.getId());
                    // Une los datos del estudiante con la nota existente, si ya fue registrada.
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
        // Obtiene IDs unicos para evitar consultas repetidas por estudiante.
        List<UUID> idsEstudiantes = inscripciones.stream()
                .map(Inscripcion::getIdEstudiante)
                .distinct()
                .toList();
        // Devuelve mapa idEstudiante -> Estudiante para acceso rapido.
        return estudianteRepository.findAllById(idsEstudiantes)
                .stream()
                .collect(Collectors.toMap(Estudiante::getId, Function.identity()));
    }

    private CalificacionAsignacionResponse toAsignacionResponse(AsignacionDocente asignacion) {
        // Construye el DTO que muestra el frontend en el selector de asignaciones.
        // Para eso une datos de docente, materia, paralelo, curso y gestion.
        UUID idInstitucion = asignacion.getIdInstitucion();

        Docente docente = docenteRepository.findByIdAndIdInstitucion(asignacion.getIdDocente(), idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Docente no encontrado: " + asignacion.getIdDocente()));
        Materia materia = materiaRepository.findByIdAndIdInstitucion(asignacion.getIdMateria(), idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Materia no encontrada: " + asignacion.getIdMateria()));
        Paralelo paralelo = paraleloRepository.findByIdAndIdInstitucion(asignacion.getIdParalelo(), idInstitucion)
                .orElseThrow(
                        () -> new EntityNotFoundException("Paralelo no encontrado: " + asignacion.getIdParalelo()));
        Curso curso = cursoRepository.findByIdAndIdInstitucion(paralelo.getIdCurso(), idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Curso no encontrado: " + paralelo.getIdCurso()));
        GestionAcademica gestion = gestionAcademicaRepository
                .findByIdAndIdInstitucion(asignacion.getIdGestion(), idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Gestion academica no encontrada: " + asignacion.getIdGestion()));

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
        // Crea un ValorNota a partir de una entidad Calificacion existente.
        static ValorNota from(Calificacion calificacion) {
            return new ValorNota(calificacion.getNotaNumerica(), calificacion.getNotaLiteral());
        }

        // Representacion comparable de la nota. Sirve para saber si una nota cambio
        // y, si cambio, registrar auditoria.
        String valorTexto() {
            return notaNumerica != null ? notaNumerica.toPlainString() : notaLiteral;
        }
    }
}
