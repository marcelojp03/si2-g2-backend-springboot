package com.uagrm.si2g2.dashboard.application;

import com.uagrm.si2g2.academico.domain.GestionAcademica;
import com.uagrm.si2g2.academico.domain.GestionAcademicaRepository;
import com.uagrm.si2g2.asignacion.domain.AsignacionDocenteRepository;
import com.uagrm.si2g2.auth.domain.UsuarioRepository;
import com.uagrm.si2g2.curso.domain.Curso;
import com.uagrm.si2g2.curso.domain.CursoRepository;
import com.uagrm.si2g2.curso.domain.Paralelo;
import com.uagrm.si2g2.curso.domain.ParaleloRepository;
import com.uagrm.si2g2.dashboard.dto.DashboardCatalogoFiltrosResponse;
import com.uagrm.si2g2.dashboard.dto.DashboardFilterOption;
import com.uagrm.si2g2.dashboard.dto.DashboardRecentInstitution;
import com.uagrm.si2g2.docente.domain.DocenteRepository;
import com.uagrm.si2g2.estudiante.domain.EstudianteRepository;
import com.uagrm.si2g2.inscripcion.domain.InscripcionRepository;
import com.uagrm.si2g2.institucion.application.ConfiguracionCatalog;
import com.uagrm.si2g2.institucion.application.ConfiguracionService;
import com.uagrm.si2g2.institucion.domain.Institucion;
import com.uagrm.si2g2.institucion.domain.InstitucionRepository;
import com.uagrm.si2g2.materia.domain.Materia;
import com.uagrm.si2g2.materia.domain.MateriaRepository;
import com.uagrm.si2g2.tutor.domain.TutorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardQueryService {

    private static final Locale LOCALE_ES = Locale.forLanguageTag("es-BO");

    private final InstitucionRepository institucionRepository;
    private final UsuarioRepository usuarioRepository;
    private final GestionAcademicaRepository gestionAcademicaRepository;
    private final CursoRepository cursoRepository;
    private final ParaleloRepository paraleloRepository;
    private final MateriaRepository materiaRepository;
    private final DocenteRepository docenteRepository;
    private final EstudianteRepository estudianteRepository;
    private final TutorRepository tutorRepository;
    private final InscripcionRepository inscripcionRepository;
    private final AsignacionDocenteRepository asignacionDocenteRepository;
    private final ConfiguracionService configuracionService;

    @Transactional(readOnly = true)
    public long totalInstituciones() {
        return institucionRepository.count();
    }

    @Transactional(readOnly = true)
    public long totalInstitucionesActivas() {
        return institucionRepository.countByEstado("ACTIVO");
    }

    @Transactional(readOnly = true)
    public long totalInstitucionesInactivas() {
        return institucionRepository.countByEstado("INACTIVO");
    }

    @Transactional(readOnly = true)
    public long totalInstitucionesPorTipo(String tipo) {
        return institucionRepository.countByTipoInstitucion(tipo);
    }

    @Transactional(readOnly = true)
    public long totalUsuariosActivos() {
        return usuarioRepository.countByEstado("ACTIVO");
    }

    @Transactional(readOnly = true)
    public List<UUID> institutionIdsWithAdmin() {
        return usuarioRepository.findInstitutionIdsWithActiveRole("ADMIN_INSTITUCION", "ACTIVO");
    }

    @Transactional(readOnly = true)
    public List<DashboardRecentInstitution> recentInstitutions() {
        return institucionRepository.findTop6ByOrderByCreadoEnDesc().stream()
                .map(inst -> new DashboardRecentInstitution(
                        inst.getId(),
                        inst.getCodigo(),
                        inst.getNombre(),
                        inst.getTipoInstitucion(),
                        inst.getEstado(),
                        inst.getDireccion(),
                        usuarioRepository.countByIdInstitucionAndEstado(inst.getId(), "ACTIVO"),
                        inst.getCreadoEn()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> institutionCreationLastMonths(int months) {
        LocalDate now = LocalDate.now();
        Map<String, Long> result = new LinkedHashMap<>();
        for (int offset = months - 1; offset >= 0; offset--) {
            LocalDate month = now.minusMonths(offset);
            String label = month.getMonth().getDisplayName(TextStyle.SHORT, LOCALE_ES) + " " + month.getYear();
            result.put(capitalize(label), 0L);
        }
        for (Institucion institucion : institucionRepository.findAll()) {
            LocalDate created = institucion.getCreadoEn().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            for (Map.Entry<String, Long> entry : result.entrySet()) {
                LocalDate keyMonth = parseMonthLabel(entry.getKey(), now.getYear());
                if (keyMonth != null && created.getYear() == keyMonth.getYear() && created.getMonth() == keyMonth.getMonth()) {
                    result.put(entry.getKey(), entry.getValue() + 1);
                    break;
                }
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Optional<GestionAcademica> gestionActiva(UUID idInstitucion) {
        return gestionAcademicaRepository.findByIdInstitucionAndActivaTrue(idInstitucion);
    }

    @Transactional(readOnly = true)
    public boolean hasGestionActiva(UUID idInstitucion) {
        return gestionAcademicaRepository.existsByIdInstitucionAndActivaTrue(idInstitucion);
    }

    @Transactional(readOnly = true)
    public long countUsuarios(UUID idInstitucion) {
        return usuarioRepository.countByIdInstitucionAndEstado(idInstitucion, "ACTIVO");
    }

    @Transactional(readOnly = true)
    public long countEstudiantes(UUID idInstitucion) {
        return estudianteRepository.countByIdInstitucionAndEstado(idInstitucion, "ACTIVO");
    }

    @Transactional(readOnly = true)
    public long countDocentes(UUID idInstitucion) {
        return docenteRepository.countByIdInstitucionAndEstado(idInstitucion, "ACTIVO");
    }

    @Transactional(readOnly = true)
    public long countTutores(UUID idInstitucion) {
        return tutorRepository.countByIdInstitucionAndEstado(idInstitucion, "ACTIVO");
    }

    @Transactional(readOnly = true)
    public long countCursos(UUID idInstitucion) {
        return cursoRepository.countByIdInstitucionAndEstado(idInstitucion, "ACTIVO");
    }

    @Transactional(readOnly = true)
    public long countParalelos(UUID idInstitucion) {
        return paraleloRepository.countByIdInstitucionAndEstado(idInstitucion, "ACTIVO");
    }

    @Transactional(readOnly = true)
    public long countMaterias(UUID idInstitucion) {
        return materiaRepository.countByIdInstitucionAndEstado(idInstitucion, "ACTIVO");
    }

    @Transactional(readOnly = true)
    public long countInscripciones(UUID idInstitucion) {
        return inscripcionRepository.countByIdInstitucionAndEstado(idInstitucion, "ACTIVA");
    }

    @Transactional(readOnly = true)
    public long countAsignaciones(UUID idInstitucion) {
        return asignacionDocenteRepository.countByIdInstitucionAndEstado(idInstitucion, "ACTIVA");
    }

    @Transactional(readOnly = true)
    public long countStudentsWithoutTutor(UUID idInstitucion) {
        return estudianteRepository.countActiveWithoutTutor(idInstitucion);
    }

    @Transactional(readOnly = true)
    public long countDocentesSinAsignacion(UUID idInstitucion) {
        long docentes = countDocentes(idInstitucion);
        long docentesAsignados = asignacionDocenteRepository.countDistinctDocentesByInstitutionAndEstado(idInstitucion, "ACTIVA");
        return Math.max(0, docentes - docentesAsignados);
    }

    @Transactional(readOnly = true)
    public long countParalelosSobreCapacidad(UUID idInstitucion) {
        long total = 0;
        for (Paralelo paralelo : paraleloRepository.findAllByIdInstitucion(idInstitucion)) {
            Integer capacidad = paralelo.getCapacidad();
            if (capacidad == null || capacidad <= 0) {
                continue;
            }
            long inscritos = inscripcionRepository.countByIdInstitucionAndIdParaleloAndEstado(idInstitucion, paralelo.getId(), "ACTIVA");
            if (inscritos > capacidad) {
                total++;
            }
        }
        return total;
    }

    @Transactional(readOnly = true)
    public long countIncompleteConfigurations(UUID idInstitucion) {
        return configuracionService.listarSoportadas(idInstitucion).stream()
                .filter(config -> config.isObligatorio())
                .filter(config -> config.getValor() == null || config.getValor().isBlank())
                .count();
    }

    @Transactional(readOnly = true)
    public List<DashboardFilterOption> gestiones(UUID idInstitucion) {
        return gestionAcademicaRepository.findAllByIdInstitucion(idInstitucion).stream()
                .map(gestion -> new DashboardFilterOption(gestion.getId().toString(), gestion.getNombre(), gestion.isActiva() ? "ACTIVA" : gestion.getEstado()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DashboardFilterOption> cursos(UUID idInstitucion) {
        return cursoRepository.findAllByIdInstitucion(idInstitucion).stream()
                .map(curso -> new DashboardFilterOption(curso.getId().toString(), curso.getNombre(), curso.getNivel()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DashboardFilterOption> paralelos(UUID idInstitucion) {
        Map<UUID, String> cursos = cursoRepository.findAllByIdInstitucion(idInstitucion).stream()
                .collect(java.util.stream.Collectors.toMap(Curso::getId, Curso::getNombre));
        return paraleloRepository.findAllByIdInstitucion(idInstitucion).stream()
                .map(paralelo -> new DashboardFilterOption(
                        paralelo.getId().toString(),
                        paralelo.getNombre(),
                        cursos.getOrDefault(paralelo.getIdCurso(), "Curso")
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DashboardFilterOption> materias(UUID idInstitucion) {
        return materiaRepository.findAllByIdInstitucion(idInstitucion).stream()
                .map(materia -> new DashboardFilterOption(materia.getId().toString(), materia.getNombre(), materia.getArea()))
                .toList();
    }

    @Transactional(readOnly = true)
    public DashboardCatalogoFiltrosResponse catalogoFiltros(UUID idInstitucion) {
        Map<String, String> configuracion = configuracionService.getResolvedConfigurationMap(idInstitucion);
        String tipoPeriodos = configuracion.getOrDefault("TIPO_PERIODOS", "BIMESTRAL");
        int cantidadPeriodos = parseInt(configuracion.getOrDefault("CANTIDAD_PERIODOS", "4"), 4);
        boolean usaTurnos = Boolean.parseBoolean(configuracion.getOrDefault("USA_TURNOS", "true"));

        List<DashboardFilterOption> turnos = usaTurnos
                ? List.of(
                new DashboardFilterOption("MANANA", "Manana", "TURNO"),
                new DashboardFilterOption("TARDE", "Tarde", "TURNO"),
                new DashboardFilterOption("NOCHE", "Noche", "TURNO")
        )
                : List.of();

        List<DashboardFilterOption> periodos = new ArrayList<>();
        String singular = switch (tipoPeriodos) {
            case "TRIMESTRAL" -> "Trimestre";
            case "SEMESTRAL" -> "Semestre";
            case "ANUAL" -> "Periodo";
            default -> "Bimestre";
        };
        for (int i = 1; i <= cantidadPeriodos; i++) {
            periodos.add(new DashboardFilterOption(String.valueOf(i), singular + " " + i, tipoPeriodos));
        }

        return new DashboardCatalogoFiltrosResponse(
                gestiones(idInstitucion),
                cursos(idInstitucion),
                paralelos(idInstitucion),
                materias(idInstitucion),
                turnos,
                periodos
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Long> matriculaPorCurso(UUID idInstitucion) {
        Map<UUID, UUID> paraleloToCurso = paraleloRepository.findAllByIdInstitucion(idInstitucion).stream()
                .collect(java.util.stream.Collectors.toMap(Paralelo::getId, Paralelo::getIdCurso));
        Map<UUID, String> cursoNombres = cursoRepository.findAllByIdInstitucion(idInstitucion).stream()
                .collect(java.util.stream.Collectors.toMap(Curso::getId, Curso::getNombre));
        Map<String, Long> result = new LinkedHashMap<>();
        cursoNombres.values().forEach(nombre -> result.put(nombre, 0L));

        for (var inscripcion : inscripcionRepository.findAllByIdInstitucion(idInstitucion)) {
            if (!"ACTIVA".equals(inscripcion.getEstado())) {
                continue;
            }
            UUID idCurso = paraleloToCurso.get(inscripcion.getIdParalelo());
            if (idCurso == null) {
                continue;
            }
            String nombreCurso = cursoNombres.get(idCurso);
            if (nombreCurso != null) {
                result.put(nombreCurso, result.getOrDefault(nombreCurso, 0L) + 1);
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Long> matriculaPorParalelo(UUID idInstitucion) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Paralelo paralelo : paraleloRepository.findAllByIdInstitucion(idInstitucion)) {
            long inscritos = inscripcionRepository.countByIdInstitucionAndIdParaleloAndEstado(idInstitucion, paralelo.getId(), "ACTIVA");
            result.put(paralelo.getNombre(), inscritos);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Long> distribucionSexo(UUID idInstitucion) {
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("Masculino", estudianteRepository.countByIdInstitucionAndSexo(idInstitucion, "MASCULINO"));
        result.put("Femenino", estudianteRepository.countByIdInstitucionAndSexo(idInstitucion, "FEMENINO"));
        result.put("Otro", estudianteRepository.countByIdInstitucionAndSexo(idInstitucion, "OTRO"));
        return result;
    }

    @Transactional(readOnly = true)
    public List<String> modulosConfigurados(UUID idInstitucion) {
        List<String> modulos = new ArrayList<>(List.of("IDENTIDAD", "ESTRUCTURA", "OPERACION", "EVALUACION", "REPORTES"));
        if (Boolean.parseBoolean(configuracionService.getResolvedConfigurationMap(idInstitucion).getOrDefault("CONTROL_ASISTENCIA_OBLIGATORIO", "true"))) {
            modulos.add("ASISTENCIA");
        }
        return modulos;
    }

    @Transactional(readOnly = true)
    public List<ConfiguracionCatalog.Definition> configuracionesObligatoriasFaltantes(UUID idInstitucion) {
        return configuracionService.listarSoportadas(idInstitucion).stream()
                .filter(config -> config.isObligatorio())
                .filter(config -> config.getValor() == null || config.getValor().isBlank())
                .map(config -> ConfiguracionCatalog.find(config.getClave()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private LocalDate parseMonthLabel(String label, int yearFallback) {
        String[] parts = label.split(" ");
        if (parts.length != 2) {
            return null;
        }
        for (java.time.Month month : java.time.Month.values()) {
            String shortName = capitalize(month.getDisplayName(TextStyle.SHORT, LOCALE_ES));
            if (shortName.equals(parts[0])) {
                return LocalDate.of(parseInt(parts[1], yearFallback), month, 1);
            }
        }
        return null;
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return value.substring(0, 1).toUpperCase(LOCALE_ES) + value.substring(1);
    }
}
