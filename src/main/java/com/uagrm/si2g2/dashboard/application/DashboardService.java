package com.uagrm.si2g2.dashboard.application;

import com.uagrm.si2g2.academico.dto.GestionAcademicaResponse;
import com.uagrm.si2g2.alertas.domain.AlertaRiesgo;
import com.uagrm.si2g2.alertas.domain.AlertaRiesgoRepository;
import com.uagrm.si2g2.asignacion.domain.AsignacionDocenteRepository;
import com.uagrm.si2g2.auditoria.application.AuditoriaService;
import com.uagrm.si2g2.auth.domain.Usuario;
import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.dashboard.dto.*;
import com.uagrm.si2g2.docente.domain.DocenteRepository;
import com.uagrm.si2g2.estudiante.domain.Estudiante;
import com.uagrm.si2g2.estudiante.domain.EstudianteRepository;
import com.uagrm.si2g2.inscripcion.domain.InscripcionRepository;
import com.uagrm.si2g2.institucion.application.ConfiguracionService;
import com.uagrm.si2g2.institucion.domain.Institucion;
import com.uagrm.si2g2.institucion.domain.InstitucionRepository;
import com.uagrm.si2g2.storage.ArchivoService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardQueryService queryService;
    private final InstitucionRepository institucionRepository;
    private final ConfiguracionService configuracionService;
    private final ArchivoService archivoService;
    private final DocenteRepository docenteRepository;
    private final AuditoriaService auditoriaService;
    private final AlertaRiesgoRepository alertaRiesgoRepository;
    private final AsignacionDocenteRepository asignacionDocenteRepository;
    private final InscripcionRepository inscripcionRepository;
    private final EstudianteRepository estudianteRepository;

    @Transactional(readOnly = true)
    public DashboardGlobalResponse getGlobalDashboard() {
        if (!SecurityUtils.currentUserHasRole("SUPER_ADMIN")) {
            throw new AccessDeniedException("Solo el super administrador puede consultar el dashboard global");
        }

        List<DashboardKpi> kpis = List.of(
                kpi("instituciones_total", "Instituciones", queryService.totalInstituciones(), "Registradas en la plataforma", "pi pi-building", "info", "/admin/instituciones"),
                kpi("instituciones_activas", "Activas", queryService.totalInstitucionesActivas(), "Instituciones operando", "pi pi-check-circle", "success", "/admin/instituciones"),
                kpi("instituciones_inactivas", "Inactivas", queryService.totalInstitucionesInactivas(), "Requieren revisión", "pi pi-pause-circle", "warn", "/admin/instituciones"),
                kpi("usuarios_activos", "Usuarios activos", queryService.totalUsuariosActivos(), "Con acceso vigente", "pi pi-users", "contrast", "/admin/usuarios"),
                kpi("config_incompleta", "Config. incompleta", countInstitutionsWithIncompleteConfig(), "Pendientes de onboarding", "pi pi-exclamation-triangle", "danger", "/admin/instituciones")
        );

        List<DashboardAlert> alerts = buildGlobalAlerts();
        auditoriaService.registrar(null, SecurityUtils.currentUserId(),
                "DASHBOARD", "GLOBAL_READ", "dashboard_global", "global",
                true, "Consulta de dashboard global");

        return new DashboardGlobalResponse(
                kpis,
                chart("instituciones_tipo", "Instituciones por tipo", "doughnut",
                        mapLabels("FISCAL", "PRIVADO", "CONVENIO"),
                        List.of(dataset("Instituciones", List.of(
                                queryService.totalInstitucionesPorTipo("FISCAL"),
                                queryService.totalInstitucionesPorTipo("PRIVADO"),
                                queryService.totalInstitucionesPorTipo("CONVENIO")
                        ), "#0f766e"))),
                chart("instituciones_estado", "Instituciones por estado", "bar",
                        mapLabels("Activas", "Inactivas"),
                        List.of(dataset("Estado", List.of(
                                queryService.totalInstitucionesActivas(),
                                queryService.totalInstitucionesInactivas()
                        ), "#1d4ed8"))),
                chartFromMap("altas_mes", "Altas recientes", "line", queryService.institutionCreationLastMonths(6), "#f59e0b"),
                alerts,
                queryService.recentInstitutions(),
                List.of(
                        action("nueva_institucion", "Nueva institucion", "Registrar un nuevo colegio", "pi pi-plus", "/admin/instituciones", "success"),
                        action("usuarios", "Gestionar usuarios", "Ver usuarios globales", "pi pi-users", "/admin/usuarios", "contrast"),
                        action("auditoria", "Auditoria", "Revisar actividad crítica", "pi pi-history", "/admin/auditoria", "warn")
                ),
                Instant.now()
        );
    }

    @Transactional(readOnly = true)
    public DashboardInstitucionalResponse getInstitutionalDashboard(Map<String, String> filters) {
        Usuario user = Optional.ofNullable(SecurityUtils.currentUser())
                .orElseThrow(() -> new AccessDeniedException("No existe usuario autenticado"));
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        Institucion institucion = institucionRepository.findById(idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Institución no encontrada: " + idInstitucion));

        DashboardInstitucionInfo info = new DashboardInstitucionInfo(
                institucion.getId(),
                institucion.getCodigo(),
                institucion.getNombre(),
                institucion.getTipoInstitucion(),
                institucion.getTelefono(),
                institucion.getCorreo(),
                institucion.getDireccion(),
                institucion.getEstado(),
                configuracionService.getResolvedConfigurationMap(idInstitucion).getOrDefault("NOMBRE_CORTO", institucion.getNombre()),
                configuracionService.getResolvedConfigurationMap(idInstitucion).getOrDefault("COLOR_PRIMARIO", "#0a2e60"),
                archivoService.obtenerPrincipal(idInstitucion, "INSTITUCION", "institucion", idInstitucion, "LOGO")
                        .map(response -> response.getUrl())
                        .orElse(null)
        );

        GestionAcademicaResponse gestionActiva = queryService.gestionActiva(idInstitucion)
                .map(GestionAcademicaResponse::from)
                .orElse(null);

        List<DashboardAlert> alerts = buildInstitutionAlerts(idInstitucion, user, gestionActiva);
        List<DashboardRiesgoAcademicoAlert> alertasRiesgo = buildAcademicRiskAlerts(
                idInstitucion, user, gestionActiva);
        if (alerts.stream().anyMatch(alert -> "danger".equals(alert.severidad()) || "warn".equals(alert.severidad()))) {
            auditoriaService.registrar(idInstitucion, user.getId(),
                    "DASHBOARD", "ALERTS_READ", "dashboard_institucional", idInstitucion.toString(),
                    true, "Consulta de dashboard institucional con alertas");
        }

        return new DashboardInstitucionalResponse(
                info,
                gestionActiva,
                sanitizeFilters(filters),
                buildInstitutionKpis(idInstitucion, gestionActiva),
                buildInstitutionCharts(idInstitucion),
                alerts,
                alertasRiesgo,
                buildPendingActions(idInstitucion, user, gestionActiva),
                buildQuickActions(user),
                queryService.modulosConfigurados(idInstitucion),
                Instant.now()
        );
    }

    @Transactional(readOnly = true)
    public DashboardCatalogoFiltrosResponse getCatalogoFiltros() {
        return queryService.catalogoFiltros(SecurityUtils.requireCurrentInstitutionId());
    }

    @Transactional(readOnly = true)
    public List<DashboardAlert> getInstitutionalAlerts(String severidad, String modulo, String estado) {
        List<DashboardAlert> alerts = buildInstitutionAlerts(
                SecurityUtils.requireCurrentInstitutionId(),
                Optional.ofNullable(SecurityUtils.currentUser()).orElseThrow(),
                queryService.gestionActiva(SecurityUtils.requireCurrentInstitutionId()).map(GestionAcademicaResponse::from).orElse(null)
        );
        return alerts.stream()
                .filter(alert -> severidad == null || severidad.equalsIgnoreCase(alert.severidad()))
                .filter(alert -> modulo == null || modulo.equalsIgnoreCase(alert.modulo()))
                .filter(alert -> estado == null || estado.equalsIgnoreCase(alert.estado()))
                .toList();
    }

    private List<DashboardKpi> buildInstitutionKpis(UUID idInstitucion, GestionAcademicaResponse gestionActiva) {
        return List.of(
                kpi("estudiantes", "Estudiantes", queryService.countEstudiantes(idInstitucion), "Activos en la institución", "pi pi-user", "info", "/estudiantes"),
                kpi("docentes", "Docentes", queryService.countDocentes(idInstitucion), "Plantel activo", "pi pi-id-card", "success", "/docentes"),
                kpi("tutores", "Tutores", queryService.countTutores(idInstitucion), "Responsables vinculados", "pi pi-users", "info", "/tutores"),
                kpi("cursos", "Cursos", queryService.countCursos(idInstitucion), "Estructura académica", "pi pi-book", "contrast", "/cursos"),
                kpi("paralelos", "Paralelos", queryService.countParalelos(idInstitucion), "Operativos por gestión", "pi pi-table", "warn", "/paralelos"),
                kpi("materias", "Materias", queryService.countMaterias(idInstitucion), "Plan curricular", "pi pi-list", "contrast", "/materias"),
                kpi("inscripciones", "Inscripciones", gestionActiva != null
                        ? queryService.countInscripciones(idInstitucion)
                        : 0L, gestionActiva != null ? "Matrícula activa" : "Sin gestión activa", "pi pi-file-edit", "success", "/inscripciones"),
                kpi("usuarios", "Usuarios", queryService.countUsuarios(idInstitucion), "Accesos institucionales", "pi pi-shield", "info", "/usuarios")
        );
    }

    private List<DashboardChart> buildInstitutionCharts(UUID idInstitucion) {
        return List.of(
                chartFromMap("matricula_curso", "Matricula por curso", "bar", queryService.matriculaPorCurso(idInstitucion), "#0f766e"),
                chartFromMap("matricula_paralelo", "Matricula por paralelo", "bar", queryService.matriculaPorParalelo(idInstitucion), "#1d4ed8"),
                chartFromMap("sexo", "Distribucion por sexo", "pie", queryService.distribucionSexo(idInstitucion), "#dc2626"),
                chart("modulos_futuros", "Modulos academicos avanzados", "doughnut",
                        List.of("Asistencia", "Calificaciones", "Horarios"),
                        List.of(dataset("Estado", List.of(0L, 0L, 0L), "#94a3b8")))
        );
    }

    private List<DashboardAlert> buildGlobalAlerts() {
        Set<UUID> institutionsWithAdmin = new HashSet<>(queryService.institutionIdsWithAdmin());
        List<DashboardAlert> alerts = new ArrayList<>();
        for (Institucion institucion : institucionRepository.findAll()) {
            if (!institutionsWithAdmin.contains(institucion.getId())) {
                alerts.add(alert("sin_admin_" + institucion.getId(), "USUARIOS", "danger",
                        "Institucion sin administrador",
                        institucion.getNombre() + " no tiene un ADMIN_INSTITUCION activo",
                        "/admin/instituciones", "ABIERTA"));
            }
            if (!queryService.hasGestionActiva(institucion.getId())) {
                alerts.add(alert("sin_gestion_" + institucion.getId(), "GESTION", "warn",
                        "Sin gestion activa",
                        institucion.getNombre() + " no tiene una gestion académica activa",
                        "/admin/instituciones", "ABIERTA"));
            }
            if (queryService.countIncompleteConfigurations(institucion.getId()) > 0) {
                alerts.add(alert("config_incompleta_" + institucion.getId(), "CONFIGURACION", "warn",
                        "Configuracion pendiente",
                        institucion.getNombre() + " tiene parámetros obligatorios por completar",
                        "/admin/instituciones", "ABIERTA"));
            }
            if (queryService.countUsuarios(institucion.getId()) == 0) {
                alerts.add(alert("sin_usuarios_" + institucion.getId(), "USUARIOS", "info",
                        "Sin actividad institucional",
                        institucion.getNombre() + " no tiene usuarios activos",
                        "/admin/usuarios", "ABIERTA"));
            }
        }
        return alerts.stream().limit(8).toList();
    }

    private List<DashboardAlert> buildInstitutionAlerts(UUID idInstitucion, Usuario user, GestionAcademicaResponse gestionActiva) {
        List<DashboardAlert> alerts = new ArrayList<>();
        long configFaltante = queryService.countIncompleteConfigurations(idInstitucion);
        if (configFaltante > 0) {
            alerts.add(alert("configuracion_incompleta", "CONFIGURACION", "warn",
                    "Configuracion institucional incompleta",
                    "Faltan " + configFaltante + " parámetros obligatorios por revisar",
                    "/configuracion", "ABIERTA"));
        }
        if (!Boolean.parseBoolean(configuracionService.getResolvedConfigurationMap(idInstitucion).getOrDefault("MATRICULA_HABILITADA", "true"))) {
            alerts.add(alert("matricula_cerrada", "OPERACION", "danger",
                    "Matricula deshabilitada",
                    "La institución tiene la matrícula deshabilitada en configuración",
                    "/configuracion", "ABIERTA"));
        }
        long sobreCapacidad = queryService.countParalelosSobreCapacidad(idInstitucion);
        if (sobreCapacidad > 0) {
            alerts.add(alert("paralelos_sobre_capacidad", "OPERACION", "warn",
                    "Paralelos sobre capacidad",
                    sobreCapacidad + " paralelos superan el cupo configurado",
                    "/paralelos", "ABIERTA"));
        }
        long docentesSinAsignacion = queryService.countDocentesSinAsignacion(idInstitucion);
        if (docentesSinAsignacion > 0 && hasRead(user, "DOCENTES_READ", "DOCENTES_UPDATE", "ASIGNACIONES_READ")) {
            alerts.add(alert("docentes_sin_asignacion", "OPERACION", "info",
                    "Docentes sin asignacion",
                    docentesSinAsignacion + " docentes activos no tienen asignación docente",
                    "/asignaciones", "ABIERTA"));
        }
        long estudiantesSinTutor = queryService.countStudentsWithoutTutor(idInstitucion);
        if (estudiantesSinTutor > 0 && hasRead(user, "ESTUDIANTES_READ", "ESTUDIANTES_UPDATE")) {
            alerts.add(alert("estudiantes_sin_tutor", "PERSONAS", "warn",
                    "Estudiantes sin tutor",
                    estudiantesSinTutor + " estudiantes activos no tienen tutor vinculado",
                    "/tutores", "ABIERTA"));
        }
        if (gestionActiva == null) {
            alerts.add(alert("sin_gestion", "GESTION", "danger",
                    "No existe gestion activa",
                    "El colegio aún no tiene una gestión académica activa",
                    "/gestiones", "ABIERTA"));
        }

        if (user.getRoles().stream().anyMatch(role -> "DOCENTE".equals(role.getCodigo()))) {
            docenteRepository.findByIdUsuarioAndIdInstitucion(user.getId(), idInstitucion).ifPresentOrElse(
                    docente -> {
                        long asignaciones = queryService.countAsignaciones(idInstitucion);
                        if (asignaciones == 0) {
                            alerts.add(alert("sin_asignaciones_docente", "MI_AREA", "info",
                                    "Mis asignaciones pendientes",
                                    "Todavía no existen asignaciones docentes activas para consulta",
                                    "/asignaciones", "ABIERTA"));
                        }
                    },
                    () -> alerts.add(alert("docente_sin_perfil", "MI_AREA", "warn",
                            "Perfil docente incompleto",
                            "Tu usuario aún no está vinculado a un registro docente",
                            "/perfil", "ABIERTA"))
            );
        }

        if (user.getRoles().stream().anyMatch(role -> "TUTOR".equals(role.getCodigo()) || "ESTUDIANTE".equals(role.getCodigo()))) {
            alerts.add(alert("modulo_futuro", "MI_AREA", "info",
                    "Portal academico en etapa futura",
                    "Asistencia, calificaciones y comunicados personales estarán disponibles cuando esos módulos se implementen",
                    "/perfil", "ABIERTA"));
        }
        return alerts;
    }

    private List<DashboardRiesgoAcademicoAlert> buildAcademicRiskAlerts(
            UUID idInstitucion, Usuario user, GestionAcademicaResponse gestionActiva) {
        if (gestionActiva == null) return List.of();
        boolean accesoInstitucional = user.getRoles().stream().anyMatch(role -> Set.of(
                "ADMIN_INSTITUCION", "DIRECTOR", "SECRETARIO", "SUPER_ADMIN").contains(role.getCodigo()));
        boolean esDocente = user.getRoles().stream().anyMatch(role -> "DOCENTE".equals(role.getCodigo()));
        if (!accesoInstitucional && !esDocente) return List.of();
        List<AlertaRiesgo> alertas = alertaRiesgoRepository
                .findByIdInstitucionAndIdGestionAcademicaAndNivelRiesgoInAndActivaTrueOrderByProcesadoEnDesc(
                        idInstitucion, gestionActiva.getId(), List.of("ALTO", "CRITICO"));
        Map<UUID, String> nombres = estudianteRepository.findAllByIdInstitucion(idInstitucion).stream()
                .collect(Collectors.toMap(Estudiante::getId,
                        estudiante -> estudiante.getNombres() + " " + estudiante.getApellidos()));
        if (accesoInstitucional) return alertas.stream()
                .map(alerta -> DashboardRiesgoAcademicoAlert.from(
                        alerta, nombres.getOrDefault(alerta.getIdEstudiante(), "Estudiante")))
                .toList();
        return docenteRepository.findByIdUsuarioAndIdInstitucion(user.getId(), idInstitucion)
                .map(docente -> {
                    Set<UUID> paralelos = asignacionDocenteRepository
                            .findAllByIdInstitucionAndIdDocente(idInstitucion, docente.getId()).stream()
                            .filter(a -> gestionActiva.getId().equals(a.getIdGestion()))
                            .filter(a -> "ACTIVA".equals(a.getEstado()))
                            .map(a -> a.getIdParalelo()).collect(Collectors.toSet());
                    Set<UUID> estudiantes = inscripcionRepository
                            .findAllByIdInstitucionAndIdGestion(idInstitucion, gestionActiva.getId()).stream()
                            .filter(i -> "ACTIVA".equals(i.getEstado()) && paralelos.contains(i.getIdParalelo()))
                            .map(i -> i.getIdEstudiante()).collect(Collectors.toSet());
                    return alertas.stream().filter(a -> estudiantes.contains(a.getIdEstudiante()))
                            .map(alerta -> DashboardRiesgoAcademicoAlert.from(
                                    alerta, nombres.getOrDefault(alerta.getIdEstudiante(), "Estudiante")))
                            .toList();
                }).orElse(List.of());
    }

    private List<DashboardAction> buildPendingActions(UUID idInstitucion, Usuario user, GestionAcademicaResponse gestionActiva) {
        List<DashboardAction> actions = new ArrayList<>();
        if (gestionActiva == null && hasRead(user, "GESTIONES_UPDATE")) {
            actions.add(action("crear_gestion", "Crear gestion activa", "Completar apertura académica", "pi pi-calendar-plus", "/gestiones", "danger"));
        }
        if (queryService.countIncompleteConfigurations(idInstitucion) > 0 && hasRead(user, "CONFIGURACION_UPDATE", "CONFIGURACION_READ")) {
            actions.add(action("revisar_config", "Revisar configuracion", "Completar parámetros obligatorios", "pi pi-cog", "/configuracion", "warn"));
        }
        if (queryService.countStudentsWithoutTutor(idInstitucion) > 0 && hasRead(user, "ESTUDIANTES_UPDATE", "ESTUDIANTES_READ")) {
            actions.add(action("vincular_tutores", "Vincular tutores", "Estudiantes sin responsable principal", "pi pi-users", "/tutores", "info"));
        }
        if (queryService.countDocentesSinAsignacion(idInstitucion) > 0 && hasRead(user, "ASIGNACIONES_UPDATE", "ASIGNACIONES_READ")) {
            actions.add(action("asignar_docentes", "Completar asignaciones", "Docentes sin materias ni paralelos", "pi pi-graduation-cap", "/asignaciones", "info"));
        }
        if (actions.isEmpty()) {
            actions.add(action("todo_ok", "Operacion estable", "No hay tareas críticas pendientes hoy", "pi pi-check-circle", "/", "success"));
        }
        return actions;
    }

    private List<DashboardAction> buildQuickActions(Usuario user) {
        List<DashboardAction> actions = new ArrayList<>();
        if (hasRead(user, "USUARIOS_READ", "USUARIOS_UPDATE")) {
            actions.add(action("usuarios", "Usuarios", "Gestionar accesos", "pi pi-users", "/usuarios", "contrast"));
        }
        if (hasRead(user, "GESTIONES_READ", "GESTIONES_UPDATE")) {
            actions.add(action("gestiones", "Gestiones", "Abrir o revisar periodos", "pi pi-calendar", "/gestiones", "info"));
            actions.add(action("cursos", "Cursos", "Revisar estructura académica", "pi pi-book", "/cursos", "info"));
        }
        if (hasRead(user, "ESTUDIANTES_READ", "ESTUDIANTES_UPDATE")) {
            actions.add(action("estudiantes", "Estudiantes", "Padron institucional", "pi pi-user-plus", "/estudiantes", "success"));
            actions.add(action("docentes", "Docentes", "Plantel académico", "pi pi-id-card", "/docentes", "success"));
        }
        if (hasRead(user, "INSCRIPCIONES_READ", "INSCRIPCIONES_UPDATE")) {
            actions.add(action("inscripciones", "Inscripciones", "Matricula y traslados", "pi pi-file-edit", "/inscripciones", "warn"));
            actions.add(action("asignaciones", "Asignaciones", "Docentes por materia y paralelo", "pi pi-link", "/asignaciones", "warn"));
        }
        if (hasRead(user, "AUDITORIA_READ")) {
            actions.add(action("auditoria", "Auditoria", "Bitácora institucional", "pi pi-history", "/auditoria", "contrast"));
        }
        if (actions.isEmpty()) {
            actions.add(action("perfil", "Mi perfil", "Acceso básico disponible", "pi pi-user", "/perfil", "info"));
        }
        return actions;
    }

    private Map<String, String> sanitizeFilters(Map<String, String> filters) {
        if (filters == null || filters.isEmpty()) {
            return Map.of();
        }
        Map<String, String> sanitized = new LinkedHashMap<>();
        for (String key : List.of("gestion", "periodo", "turno", "curso", "paralelo", "materia", "nivel")) {
            if (filters.containsKey(key) && filters.get(key) != null && !filters.get(key).isBlank()) {
                sanitized.put(key, filters.get(key));
            }
        }
        return sanitized;
    }

    private long countInstitutionsWithIncompleteConfig() {
        return institucionRepository.findAll().stream()
                .filter(inst -> queryService.countIncompleteConfigurations(inst.getId()) > 0)
                .count();
    }

    private boolean hasRead(Usuario user, String... permissions) {
        Set<String> authorities = new HashSet<>();
        user.getAuthorities().forEach(authority -> authorities.add(authority.getAuthority()));
        if (authorities.contains("ROLE_SUPER_ADMIN")) {
            return true;
        }
        return Arrays.stream(permissions).anyMatch(authorities::contains);
    }

    private DashboardKpi kpi(String codigo, String titulo, long valor, String subtitulo, String icono, String severidad, String ruta) {
        return new DashboardKpi(codigo, titulo, String.valueOf(valor), subtitulo, icono, severidad, ruta);
    }

    private DashboardAction action(String codigo, String titulo, String descripcion, String icono, String ruta, String severidad) {
        return new DashboardAction(codigo, titulo, descripcion, icono, ruta, severidad);
    }

    private DashboardAlert alert(String id, String modulo, String severidad, String titulo, String detalle, String ruta, String estado) {
        return new DashboardAlert(id, modulo, severidad, titulo, detalle, ruta, estado);
    }

    private DashboardChart chart(String codigo, String titulo, String tipo, List<String> labels, List<DashboardDataset> datasets) {
        return new DashboardChart(codigo, titulo, tipo, labels, datasets);
    }

    private DashboardChart chartFromMap(String codigo, String titulo, String tipo, Map<String, Long> data, String color) {
        return chart(codigo, titulo, tipo, new ArrayList<>(data.keySet()), List.of(dataset(titulo, new ArrayList<>(data.values()), color)));
    }

    private DashboardDataset dataset(String label, List<Long> data, String color) {
        return new DashboardDataset(label, data, color);
    }

    private List<String> mapLabels(String... labels) {
        return List.of(labels);
    }
}
