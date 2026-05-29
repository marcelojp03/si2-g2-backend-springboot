package com.uagrm.si2g2.asistencia.application;

import com.uagrm.si2g2.academico.domain.GestionAcademica;
import com.uagrm.si2g2.academico.domain.GestionAcademicaRepository;
import com.uagrm.si2g2.asignacion.domain.AsignacionDocente;
import com.uagrm.si2g2.asignacion.domain.AsignacionDocenteRepository;
import com.uagrm.si2g2.asistencia.domain.AsistenciaDetalle;
import com.uagrm.si2g2.asistencia.domain.AsistenciaDetalleRepository;
import com.uagrm.si2g2.asistencia.domain.AsistenciaRegistro;
import com.uagrm.si2g2.asistencia.domain.AsistenciaRegistroRepository;
import com.uagrm.si2g2.asistencia.dto.*;
import com.uagrm.si2g2.auditoria.application.AuditoriaService;
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
import com.uagrm.si2g2.materia.domain.Materia;
import com.uagrm.si2g2.materia.domain.MateriaRepository;
import com.uagrm.si2g2.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AsistenciaService {

    private static final String ESTADO_ASIGNACION_ACTIVA = "ACTIVA";
    private static final String ESTADO_INSCRIPCION_ACTIVA = "ACTIVA";

    private static final Set<String> ESTADOS_ASISTENCIA_VALIDOS = Set.of(
            "PRESENTE",
            "AUSENTE",
            "TARDANZA",
            "JUSTIFICADO"
    );

    private final AsistenciaRegistroRepository registroRepository;
    private final AsistenciaDetalleRepository detalleRepository;

    private final AsignacionDocenteRepository asignacionDocenteRepository;
    private final InscripcionRepository inscripcionRepository;
    private final EstudianteRepository estudianteRepository;
    private final DocenteRepository docenteRepository;
    private final MateriaRepository materiaRepository;
    private final ParaleloRepository paraleloRepository;
    private final CursoRepository cursoRepository;
    private final GestionAcademicaRepository gestionAcademicaRepository;

    private final AuditoriaService auditoriaService;

    @Transactional(readOnly = true)
    public List<AsistenciaAsignacionResponse> listarMisAsignaciones() {
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
    public AsistenciaPlantillaResponse obtenerPlantilla(UUID idAsignacionDocente, LocalDate fecha) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();

        validarFecha(fecha);
        AsignacionDocente asignacion = buscarAsignacionActiva(idAsignacionDocente, idInstitucion);
        validarAccesoLectura(asignacion);

        AsistenciaRegistro registro = registroRepository
                .findByIdInstitucionAndIdAsignacionDocenteAndFecha(idInstitucion, idAsignacionDocente, fecha)
                .orElse(null);

        List<Inscripcion> inscripcionesActivas = obtenerInscripcionesActivasDeAsignacion(idInstitucion, asignacion);

        Map<UUID, AsistenciaDetalle> detallesPorInscripcion = registro == null
                ? Map.of()
                : detalleRepository.findAllByIdAsistenciaRegistro(registro.getId())
                        .stream()
                        .collect(Collectors.toMap(AsistenciaDetalle::getIdInscripcion, Function.identity()));

        List<AsistenciaEstudianteResponse> estudiantes = construirEstudiantesResponse(
                inscripcionesActivas,
                detallesPorInscripcion
        );

        return AsistenciaPlantillaResponse.builder()
                .idAsistenciaRegistro(registro == null ? null : registro.getId())
                .idAsignacionDocente(idAsignacionDocente)
                .fecha(fecha)
                .estadoRegistro(registro == null ? "NO_REGISTRADA" : registro.getEstado())
                .registrada(registro != null)
                .asignacion(toAsignacionResponse(asignacion))
                .estudiantes(estudiantes)
                .totalEstudiantes(estudiantes.size())
                .build();
    }

    @Transactional
    public AsistenciaRegistroResponse guardar(AsistenciaRegistroRequest request) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();

        validarFecha(request.getFecha());
        validarFechaPasada(request.getFecha());

        AsignacionDocente asignacion = buscarAsignacionActiva(request.getIdAsignacionDocente(), idInstitucion);
        validarAccesoEscritura(asignacion);
        validarFechaDentroGestion(asignacion, request.getFecha(), idInstitucion);

        List<Inscripcion> inscripcionesActivas = obtenerInscripcionesActivasDeAsignacion(idInstitucion, asignacion);
        validarDetallesContraInscripcionesActivas(request.getDetalles(), inscripcionesActivas);

        Optional<AsistenciaRegistro> existente = registroRepository
                .findByIdInstitucionAndIdAsignacionDocenteAndFecha(
                        idInstitucion,
                        request.getIdAsignacionDocente(),
                        request.getFecha()
                );

        AsistenciaRegistro registro = existente.orElseGet(() -> AsistenciaRegistro.builder()
                .idInstitucion(idInstitucion)
                .idAsignacionDocente(request.getIdAsignacionDocente())
                .fecha(request.getFecha())
                .registradoPor(SecurityUtils.currentUserId())
                .estado("REGISTRADA")
                .build());

        boolean esActualizacion = existente.isPresent();
        if (esActualizacion) {
            registro.setEstado("MODIFICADA");
            registro.setRegistradoPor(SecurityUtils.currentUserId());
        }

        registro = registroRepository.save(registro);

        Map<UUID, AsistenciaDetalle> detallesExistentes = detalleRepository
                .findAllByIdAsistenciaRegistro(registro.getId())
                .stream()
                .collect(Collectors.toMap(AsistenciaDetalle::getIdInscripcion, Function.identity()));

        List<AsistenciaDetalle> detallesGuardar = new ArrayList<>();

        for (AsistenciaDetalleRequest detalleRequest : request.getDetalles()) {
            String estadoNormalizado = normalizarEstadoAsistencia(detalleRequest.getEstadoAsistencia());

            AsistenciaDetalle detalle = detallesExistentes.get(detalleRequest.getIdInscripcion());

            if (detalle == null) {
                detalle = AsistenciaDetalle.builder()
                        .idAsistenciaRegistro(registro.getId())
                        .idInscripcion(detalleRequest.getIdInscripcion())
                        .estadoAsistencia(estadoNormalizado)
                        .build();
            } else {
                detalle.setEstadoAsistencia(estadoNormalizado);
            }

            detallesGuardar.add(detalle);
        }

        detalleRepository.saveAll(detallesGuardar);

        auditoriaService.registrar(
                idInstitucion,
                SecurityUtils.currentUserId(),
                "ASISTENCIA",
                esActualizacion ? "ACTUALIZAR" : "REGISTRAR",
                "asistencia_registro",
                registro.getId().toString(),
                true,
                esActualizacion ? "Asistencia modificada" : "Asistencia registrada"
        );

        return obtener(registro.getId());
    }

    @Transactional(readOnly = true)
    public AsistenciaRegistroResponse obtener(UUID id) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();

        AsistenciaRegistro registro = registroRepository.findByIdAndIdInstitucion(id, idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Asistencia no encontrada: " + id));

        AsignacionDocente asignacion = buscarAsignacion(registro.getIdAsignacionDocente(), idInstitucion);
        validarAccesoLectura(asignacion);

        List<AsistenciaDetalle> detalles = detalleRepository.findAllByIdAsistenciaRegistro(registro.getId());

        List<UUID> idsInscripcion = detalles.stream()
                .map(AsistenciaDetalle::getIdInscripcion)
                .toList();

        Map<UUID, Inscripcion> inscripcionesPorId = inscripcionRepository.findAllById(idsInscripcion)
                .stream()
                .collect(Collectors.toMap(Inscripcion::getId, Function.identity()));

        Map<UUID, AsistenciaDetalle> detallesPorInscripcion = detalles.stream()
                .collect(Collectors.toMap(AsistenciaDetalle::getIdInscripcion, Function.identity()));

        List<Inscripcion> inscripcionesOrdenadas = idsInscripcion.stream()
                .map(inscripcionesPorId::get)
                .filter(Objects::nonNull)
                .toList();

        List<AsistenciaEstudianteResponse> estudiantes = construirEstudiantesResponse(
                inscripcionesOrdenadas,
                detallesPorInscripcion
        );

        return AsistenciaRegistroResponse.builder()
                .id(registro.getId())
                .idInstitucion(registro.getIdInstitucion())
                .idAsignacionDocente(registro.getIdAsignacionDocente())
                .registradoPor(registro.getRegistradoPor())
                .fecha(registro.getFecha())
                .estado(registro.getEstado())
                .asignacion(toAsignacionResponse(asignacion))
                .detalles(estudiantes)
                .totalPresentes(contar(estudiantes, "PRESENTE"))
                .totalAusentes(contar(estudiantes, "AUSENTE"))
                .totalTardanzas(contar(estudiantes, "TARDANZA"))
                .totalJustificados(contar(estudiantes, "JUSTIFICADO"))
                .totalEstudiantes(estudiantes.size())
                .creadoEn(registro.getCreadoEn())
                .actualizadoEn(registro.getActualizadoEn())
                .build();
    }

    private AsignacionDocente buscarAsignacionActiva(UUID idAsignacionDocente, UUID idInstitucion) {
        AsignacionDocente asignacion = buscarAsignacion(idAsignacionDocente, idInstitucion);
        if (!ESTADO_ASIGNACION_ACTIVA.equals(asignacion.getEstado())) {
            throw new IllegalStateException("La asignación docente no está activa");
        }
        return asignacion;
    }

    private AsignacionDocente buscarAsignacion(UUID idAsignacionDocente, UUID idInstitucion) {
        return asignacionDocenteRepository.findByIdAndIdInstitucion(idAsignacionDocente, idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Asignación docente no encontrada: " + idAsignacionDocente));
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

    private void validarDetallesContraInscripcionesActivas(
            List<AsistenciaDetalleRequest> detalles,
            List<Inscripcion> inscripcionesActivas
    ) {
        if (inscripcionesActivas.isEmpty()) {
            throw new IllegalStateException("No existen estudiantes inscritos activos para esta asignación");
        }

        Set<UUID> idsInscripcionesActivas = inscripcionesActivas.stream()
                .map(Inscripcion::getId)
                .collect(Collectors.toSet());

        Set<UUID> idsRecibidos = new HashSet<>();

        for (AsistenciaDetalleRequest detalle : detalles) {
            if (!idsRecibidos.add(detalle.getIdInscripcion())) {
                throw new IllegalArgumentException("La inscripción está duplicada en la asistencia: " + detalle.getIdInscripcion());
            }

            if (!idsInscripcionesActivas.contains(detalle.getIdInscripcion())) {
                throw new IllegalArgumentException("La inscripción no pertenece al paralelo y gestión de la asignación: " + detalle.getIdInscripcion());
            }

            normalizarEstadoAsistencia(detalle.getEstadoAsistencia());
        }

        if (!idsRecibidos.equals(idsInscripcionesActivas)) {
            throw new IllegalStateException("La asistencia debe incluir todos los estudiantes inscritos activos");
        }
    }

    private String normalizarEstadoAsistencia(String estado) {
        if (estado == null || estado.isBlank()) {
            throw new IllegalArgumentException("El estado de asistencia es obligatorio");
        }

        String normalizado = estado.trim().toUpperCase();

        if (!ESTADOS_ASISTENCIA_VALIDOS.contains(normalizado)) {
            throw new IllegalArgumentException("Estado de asistencia inválido: " + estado);
        }

        return normalizado;
    }

    private void validarFecha(LocalDate fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha es obligatoria");
        }

        if (fecha.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("No se puede registrar asistencia en una fecha futura");
        }
    }

    private void validarFechaPasada(LocalDate fecha) {
        if (!fecha.isBefore(LocalDate.now())) {
            return;
        }

        boolean puedeEditarPasado = SecurityUtils.currentUserHasAuthority("ASISTENCIA_BACKDATE")
                || SecurityUtils.currentUserHasRole("ADMIN_INSTITUCION")
                || SecurityUtils.currentUserHasRole("SUPER_ADMIN");

        if (!puedeEditarPasado) {
            throw new AccessDeniedException("No tienes permiso para registrar o modificar asistencia de fechas pasadas");
        }
    }

    private void validarFechaDentroGestion(AsignacionDocente asignacion, LocalDate fecha, UUID idInstitucion) {
        GestionAcademica gestion = gestionAcademicaRepository
                .findByIdAndIdInstitucion(asignacion.getIdGestion(), idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Gestión académica no encontrada: " + asignacion.getIdGestion()));

        if (fecha.isBefore(gestion.getFechaInicio()) || fecha.isAfter(gestion.getFechaFin())) {
            throw new IllegalArgumentException("La fecha de asistencia está fuera del rango de la gestión académica");
        }
    }

    private void validarAccesoLectura(AsignacionDocente asignacion) {
        if (puedeConsultarTodo()) {
            return;
        }

        validarDocentePropietario(asignacion);
    }

    private void validarAccesoEscritura(AsignacionDocente asignacion) {
        if (SecurityUtils.currentUserHasRole("ADMIN_INSTITUCION")
                || SecurityUtils.currentUserHasRole("SUPER_ADMIN")
                || SecurityUtils.currentUserHasRole("DIRECTOR")
                || SecurityUtils.currentUserHasAuthority("ASISTENCIA_WRITE")) {
            return;
        }

        validarDocentePropietario(asignacion);
    }

    private boolean puedeConsultarTodo() {
        return SecurityUtils.currentUserHasRole("ADMIN_INSTITUCION")
                || SecurityUtils.currentUserHasRole("SUPER_ADMIN")
                || SecurityUtils.currentUserHasRole("DIRECTOR")
                || SecurityUtils.currentUserHasAuthority("ASISTENCIA_READ_ALL");
    }

    private void validarDocentePropietario(AsignacionDocente asignacion) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        UUID idUsuario = SecurityUtils.currentUserId();

        Docente docente = docenteRepository.findByIdUsuarioAndIdInstitucion(idUsuario, idInstitucion)
                .orElseThrow(() -> new AccessDeniedException("El usuario autenticado no tiene docente asociado"));

        if (!docente.getId().equals(asignacion.getIdDocente())) {
            throw new AccessDeniedException("No puedes acceder a la asistencia de una asignación que no te pertenece");
        }
    }

    private List<AsistenciaEstudianteResponse> construirEstudiantesResponse(
            List<Inscripcion> inscripciones,
            Map<UUID, AsistenciaDetalle> detallesPorInscripcion
    ) {
        List<UUID> idsEstudiantes = inscripciones.stream()
                .map(Inscripcion::getIdEstudiante)
                .distinct()
                .toList();

        Map<UUID, Estudiante> estudiantesPorId = estudianteRepository.findAllById(idsEstudiantes)
                .stream()
                .collect(Collectors.toMap(Estudiante::getId, Function.identity()));

        return inscripciones.stream()
                .map(inscripcion -> {
                    Estudiante estudiante = estudiantesPorId.get(inscripcion.getIdEstudiante());
                    if (estudiante == null) {
                        throw new EntityNotFoundException("Estudiante no encontrado: " + inscripcion.getIdEstudiante());
                    }

                    AsistenciaDetalle detalle = detallesPorInscripcion.get(inscripcion.getId());

                    return AsistenciaEstudianteResponse.builder()
                            .idDetalle(detalle == null ? null : detalle.getId())
                            .idInscripcion(inscripcion.getId())
                            .idEstudiante(estudiante.getId())
                            .codigoEstudiante(estudiante.getCodigoEstudiante())
                            .documentoIdentidad(estudiante.getDocumentoIdentidad())
                            .nombres(estudiante.getNombres())
                            .apellidos(estudiante.getApellidos())
                            .nombreCompleto(estudiante.getNombres() + " " + estudiante.getApellidos())
                            .estadoAsistencia(detalle == null ? "PRESENTE" : detalle.getEstadoAsistencia())
                            .registrado(detalle != null)
                            .build();
                })
                .sorted(Comparator.comparing(AsistenciaEstudianteResponse::getApellidos)
                        .thenComparing(AsistenciaEstudianteResponse::getNombres))
                .toList();
    }

    private AsistenciaAsignacionResponse toAsignacionResponse(AsignacionDocente asignacion) {
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
                .orElseThrow(() -> new EntityNotFoundException("Gestión académica no encontrada: " + asignacion.getIdGestion()));

        return AsistenciaAsignacionResponse.builder()
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

    private long contar(List<AsistenciaEstudianteResponse> estudiantes, String estado) {
        return estudiantes.stream()
                .filter(e -> estado.equals(e.getEstadoAsistencia()))
                .count();
    }
}