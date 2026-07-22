package com.uagrm.si2g2.alertas.application;

import com.uagrm.si2g2.alertas.domain.AlertaRiesgo;
import com.uagrm.si2g2.alertas.domain.AlertaRiesgoRepository;
import com.uagrm.si2g2.alertas.domain.RecomendacionIaRepository;
import com.uagrm.si2g2.alertas.domain.AlertaRiesgoSeguimiento;
import com.uagrm.si2g2.alertas.domain.AlertaRiesgoSeguimientoRepository;
import com.uagrm.si2g2.alertas.dto.AlertaRiesgoResponse;
import com.uagrm.si2g2.alertas.dto.AlertaRiesgoSeguimientoResponse;
import com.uagrm.si2g2.alertas.dto.RecomendacionIaResponse;
import com.uagrm.si2g2.asignacion.domain.AsignacionDocenteRepository;
import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.auth.domain.UsuarioRepository;
import com.uagrm.si2g2.docente.domain.DocenteRepository;
import com.uagrm.si2g2.estudiante.domain.EstudianteRepository;
import com.uagrm.si2g2.estudiante.domain.Estudiante;
import com.uagrm.si2g2.inscripcion.domain.InscripcionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlertaRiesgoService {
    private static final Set<String> ESTADOS = Set.of("ABIERTA", "EN_SEGUIMIENTO", "ATENDIDA", "CERRADA");
    private static final Map<String, String> SIGUIENTE_ESTADO = Map.of(
            "ABIERTA", "EN_SEGUIMIENTO", "EN_SEGUIMIENTO", "ATENDIDA", "ATENDIDA", "CERRADA");

    private final AlertaRiesgoRepository repository;
    private final RecomendacionIaRepository recomendacionRepository;
    private final AlertaRiesgoSeguimientoRepository seguimientoRepository;
    private final UsuarioRepository usuarioRepository;
    private final DocenteRepository docenteRepository;
    private final AsignacionDocenteRepository asignacionRepository;
    private final InscripcionRepository inscripcionRepository;
    private final EstudianteRepository estudianteRepository;

    @Transactional(readOnly = true)
    public List<AlertaRiesgoResponse> listar(UUID idInstitucion, UUID idGestion, String nivel, Boolean activa) {
        validarTenantActual(idInstitucion);
        return prepararListado(
                repository.buscarConFiltros(idInstitucion, idGestion, nivel, activa),
                idInstitucion, idGestion);
    }

    @Transactional(readOnly = true)
    public List<AlertaRiesgoResponse> listar(UUID idInstitucion, UUID idGestion,
                                             UUID idCurso, UUID idParalelo,
                                             UUID idMateria,
                                             String nivel, Boolean activa) {
        validarTenantActual(idInstitucion);
        List<AlertaRiesgo> alertas = idCurso == null && idParalelo == null && idMateria == null
                ? repository.buscarConFiltros(idInstitucion, idGestion, nivel, activa)
                : repository.buscarConFiltrosAcademicos(
                        idInstitucion, idGestion, idCurso, idParalelo, idMateria, nivel, activa);
        return prepararListado(alertas, idInstitucion, idGestion);
    }

    private List<AlertaRiesgoResponse> prepararListado(
            List<AlertaRiesgo> alertas, UUID idInstitucion, UUID idGestion) {
        if (SecurityUtils.currentUserHasRole("DOCENTE") && !tieneAccesoInstitucional()) {
            UUID idDocente = docenteRepository
                    .findByIdUsuarioAndIdInstitucion(SecurityUtils.currentUserId(), idInstitucion)
                    .orElseThrow(() -> new EntityNotFoundException("El usuario no tiene docente asociado"))
                    .getId();
            Set<AccesoParalelo> asignaciones = asignacionRepository
                    .findAllByIdInstitucionAndIdDocente(idInstitucion, idDocente).stream()
                    .filter(a -> "ACTIVA".equals(a.getEstado()))
                    .filter(a -> idGestion == null || idGestion.equals(a.getIdGestion()))
                    .map(a -> new AccesoParalelo(a.getIdGestion(), a.getIdParalelo()))
                    .collect(Collectors.toSet());
            Set<AccesoEstudiante> accesos = inscripcionRepository.findAllByIdInstitucion(idInstitucion).stream()
                    .filter(i -> "ACTIVA".equals(i.getEstado()))
                    .filter(i -> idGestion == null || idGestion.equals(i.getIdGestion()))
                    .filter(i -> asignaciones.contains(new AccesoParalelo(i.getIdGestion(), i.getIdParalelo())))
                    .map(i -> new AccesoEstudiante(i.getIdGestion(), i.getIdEstudiante()))
                    .collect(Collectors.toSet());
            alertas = alertas.stream().filter(a -> accesos.contains(
                    new AccesoEstudiante(a.getIdGestionAcademica(), a.getIdEstudiante()))).toList();
        }
        Map<UUID, Estudiante> estudiantes = estudianteRepository.findAllByIdInstitucion(idInstitucion).stream()
                .collect(Collectors.toMap(Estudiante::getId, estudiante -> estudiante));
        return alertas.stream().map(alerta -> respuesta(alerta, estudiantes.get(alerta.getIdEstudiante()))).toList();
    }

    @Transactional(readOnly = true)
    public AlertaRiesgoResponse obtener(UUID id, UUID idInstitucion) {
        validarTenantActual(idInstitucion);
        AlertaRiesgo alerta = obtenerConAcceso(id, idInstitucion);
        return respuesta(alerta, buscarEstudiante(alerta, idInstitucion));
    }

    @Transactional(readOnly = true)
    public List<AlertaRiesgoResponse> listarPorEstudiante(UUID idEstudiante) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        Estudiante estudiante = estudianteRepository.findByIdAndIdInstitucion(idEstudiante, idInstitucion).orElse(null);
        return repository.findByIdInstitucionAndIdEstudianteAndActivaTrue(idInstitucion, idEstudiante)
                .stream().map(alerta -> respuesta(alerta, estudiante)).toList();
    }

    @Transactional(readOnly = true)
    public List<RecomendacionIaResponse> recomendaciones(UUID idAlerta) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        obtenerConAcceso(idAlerta, idInstitucion);
        return recomendacionRepository.findByAlertaAndInstitucion(idAlerta, idInstitucion)
                .stream().map(RecomendacionIaResponse::from).toList();
    }

    @Transactional
    public AlertaRiesgoResponse actualizarEstado(UUID idAlerta, String estado, String observacion) {
        String normalizado = estado == null ? "" : estado.trim().toUpperCase(Locale.ROOT);
        if (!ESTADOS.contains(normalizado)) throw new IllegalArgumentException("Estado de alerta invalido");
        if (("ATENDIDA".equals(normalizado) || "CERRADA".equals(normalizado))
                && (observacion == null || observacion.isBlank())) {
            throw new IllegalArgumentException("La observacion es obligatoria para estados ATENDIDA y CERRADA");
        }
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        UUID idEstudiante = repository.findIdEstudianteByIdAndIdInstitucion(idAlerta, idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Alerta no encontrada: " + idAlerta));
        Estudiante estudiante = estudianteRepository.findByIdAndIdInstitucionForUpdate(idEstudiante, idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Estudiante no encontrado"));
        AlertaRiesgo alerta = repository.findByIdAndIdInstitucionForUpdate(idAlerta, idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Alerta no encontrada: " + idAlerta));
        validarAcceso(alerta, idInstitucion);
        String anterior = alerta.getEstadoAlerta();
        if (normalizado.equals(anterior)) return respuesta(alerta, estudiante);
        if (!normalizado.equals(SIGUIENTE_ESTADO.get(anterior))) {
            throw new IllegalStateException("Transicion de estado no permitida: " + anterior + " -> " + normalizado);
        }
        alerta.setEstadoAlerta(normalizado);
        if ("CERRADA".equals(normalizado)) alerta.setActiva(false);
        AlertaRiesgo guardada = repository.save(alerta);
        seguimientoRepository.save(AlertaRiesgoSeguimiento.builder()
                .idAlertaRiesgo(idAlerta).idInstitucion(idInstitucion).estadoAnterior(anterior)
                .estadoNuevo(normalizado).observacion(normalizarObservacion(observacion))
                .idUsuario(SecurityUtils.currentUserId()).build());
        return respuesta(guardada, estudiante);
    }

    @Transactional(readOnly = true)
    public List<AlertaRiesgoSeguimientoResponse> seguimientos(UUID idAlerta) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        obtenerConAcceso(idAlerta, idInstitucion);
        return seguimientoRepository
                .findAllByIdAlertaRiesgoAndIdInstitucionOrderByCreadoEnAscIdAsc(idAlerta, idInstitucion).stream()
                .map(seguimiento -> AlertaRiesgoSeguimientoResponse.from(seguimiento,
                        nombreUsuario(seguimiento.getIdUsuario(), idInstitucion))).toList();
    }

    private AlertaRiesgo obtenerConAcceso(UUID idAlerta, UUID idInstitucion) {
        AlertaRiesgo alerta = repository.findByIdAndIdInstitucion(idAlerta, idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Alerta no encontrada: " + idAlerta));
        validarAcceso(alerta, idInstitucion);
        return alerta;
    }

    private void validarAcceso(AlertaRiesgo alerta, UUID idInstitucion) {
        if (tieneAccesoInstitucional()) return;
        if (!SecurityUtils.currentUserHasRole("DOCENTE")) {
            throw new AccessDeniedException("No tienes acceso a la alerta");
        }
        UUID idDocente = docenteRepository
                .findByIdUsuarioAndIdInstitucion(SecurityUtils.currentUserId(), idInstitucion)
                .orElseThrow(() -> new AccessDeniedException("El usuario autenticado no tiene docente asociado"))
                .getId();
        boolean asignado = inscripcionRepository
                .findAllByIdInstitucionAndIdEstudiante(idInstitucion, alerta.getIdEstudiante()).stream()
                .filter(i -> alerta.getIdGestionAcademica().equals(i.getIdGestion()))
                .filter(i -> "ACTIVA".equals(i.getEstado()))
                .anyMatch(i -> asignacionRepository
                        .existsByIdInstitucionAndIdDocenteAndIdParaleloAndIdGestionAndEstado(
                                idInstitucion, idDocente, i.getIdParalelo(), alerta.getIdGestionAcademica(), "ACTIVA"));
        if (!asignado) throw new AccessDeniedException("No tienes una asignacion activa para el paralelo de la alerta");
    }

    private Estudiante buscarEstudiante(AlertaRiesgo alerta, UUID idInstitucion) {
        return estudianteRepository.findByIdAndIdInstitucion(alerta.getIdEstudiante(), idInstitucion).orElse(null);
    }

    private AlertaRiesgoResponse respuesta(AlertaRiesgo alerta, Estudiante estudiante) {
        return AlertaRiesgoResponse.from(alerta, estudiante);
    }

    private void validarTenantActual(UUID idInstitucion) {
        if (!SecurityUtils.requireCurrentInstitutionId().equals(idInstitucion)) {
            throw new AccessDeniedException("La institucion solicitada no corresponde al usuario autenticado");
        }
    }

    private String nombreUsuario(UUID idUsuario, UUID idInstitucion) {
        if (idUsuario == null) return "Sistema";
        return usuarioRepository.findByIdAndIdInstitucion(idUsuario, idInstitucion)
                .map(usuario -> usuario.getNombres() + " " + usuario.getApellidos()).orElse("Usuario no disponible");
    }

    private String normalizarObservacion(String observacion) {
        return observacion == null || observacion.isBlank() ? null : observacion.trim();
    }

    private boolean tieneAccesoInstitucional() {
        return SecurityUtils.currentUserHasRole("ADMIN_INSTITUCION")
                || SecurityUtils.currentUserHasRole("DIRECTOR")
                || SecurityUtils.currentUserHasRole("SECRETARIO")
                || SecurityUtils.currentUserHasRole("SUPER_ADMIN");
    }

    private record AccesoEstudiante(UUID idGestion, UUID idEstudiante) {
    }

    private record AccesoParalelo(UUID idGestion, UUID idParalelo) {
    }
}
