package com.uagrm.si2g2.seed.application;

import com.uagrm.si2g2.academico.domain.GestionAcademica;
import com.uagrm.si2g2.academico.domain.GestionAcademicaRepository;
import com.uagrm.si2g2.asignacion.domain.AsignacionDocente;
import com.uagrm.si2g2.asignacion.domain.AsignacionDocenteRepository;
import com.uagrm.si2g2.asistencia.domain.AsistenciaDetalle;
import com.uagrm.si2g2.asistencia.domain.AsistenciaDetalleRepository;
import com.uagrm.si2g2.asistencia.domain.AsistenciaRegistro;
import com.uagrm.si2g2.asistencia.domain.AsistenciaRegistroRepository;
import com.uagrm.si2g2.aula.domain.Aula;
import com.uagrm.si2g2.aula.domain.AulaRepository;
import com.uagrm.si2g2.auth.domain.Rol;
import com.uagrm.si2g2.auth.domain.RolRepository;
import com.uagrm.si2g2.auth.domain.Usuario;
import com.uagrm.si2g2.auth.domain.UsuarioRepository;
import com.uagrm.si2g2.calificacion.domain.ActividadEvaluativa;
import com.uagrm.si2g2.calificacion.domain.ActividadEvaluativaRepository;
import com.uagrm.si2g2.calificacion.domain.AutoevaluacionTrimestral;
import com.uagrm.si2g2.calificacion.domain.AutoevaluacionTrimestralRepository;
import com.uagrm.si2g2.calificacion.domain.CalificacionActividad;
import com.uagrm.si2g2.calificacion.domain.CalificacionActividadRepository;
import com.uagrm.si2g2.calificacion.domain.CalificacionSer;
import com.uagrm.si2g2.calificacion.domain.CalificacionSerRepository;
import com.uagrm.si2g2.calificacion.domain.EvaluacionMateria;
import com.uagrm.si2g2.calificacion.domain.EvaluacionMateriaRepository;
import com.uagrm.si2g2.calificacion.domain.PeriodoEvaluacion;
import com.uagrm.si2g2.calificacion.domain.PeriodoEvaluacionRepository;
import com.uagrm.si2g2.curso.domain.Curso;
import com.uagrm.si2g2.curso.domain.CursoRepository;
import com.uagrm.si2g2.curso.domain.Paralelo;
import com.uagrm.si2g2.curso.domain.ParaleloRepository;
import com.uagrm.si2g2.docente.domain.Docente;
import com.uagrm.si2g2.docente.domain.DocenteRepository;
import com.uagrm.si2g2.estudiante.domain.Estudiante;
import com.uagrm.si2g2.estudiante.domain.EstudianteRepository;
import com.uagrm.si2g2.horario.domain.HorarioClase;
import com.uagrm.si2g2.horario.domain.HorarioClaseRepository;
import com.uagrm.si2g2.inscripcion.domain.Inscripcion;
import com.uagrm.si2g2.inscripcion.domain.InscripcionRepository;
import com.uagrm.si2g2.institucion.application.ConfiguracionCatalog;
import com.uagrm.si2g2.institucion.domain.ConfiguracionInstitucion;
import com.uagrm.si2g2.institucion.domain.ConfiguracionInstitucionRepository;
import com.uagrm.si2g2.institucion.domain.Institucion;
import com.uagrm.si2g2.institucion.domain.InstitucionRepository;
import com.uagrm.si2g2.materia.domain.Materia;
import com.uagrm.si2g2.materia.domain.MateriaRepository;
import com.uagrm.si2g2.seed.dto.SeedResult;
import com.uagrm.si2g2.seed.dto.SeedUser;
import com.uagrm.si2g2.tutor.domain.EstudianteTutor;
import com.uagrm.si2g2.tutor.domain.EstudianteTutorRepository;
import com.uagrm.si2g2.tutor.domain.Tutor;
import com.uagrm.si2g2.tutor.domain.TutorRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyntheticDataSeeder {

    private static final String PASSWORD = "Colegio2026!";
    private static final String INSTITUCION_CODIGO = "CSM-001";
    private static final String INSTITUCION_NOMBRE = "Colegio San Miguel";
    private static final String INSTITUCION_TIPO = "PRIVADO";
    private static final String CIUDAD = "Santa Cruz";
    private static final String DIRECCION = "Av. Centro #456, Santa Cruz - Bolivia";
    private static final String DOMINIO = "sanmiguel.edu.bo";
    private static final String TELEFONO = "33112345";
    private static final String GESTION_NOMBRE = "Gestion Academica 2026";

    private static final String[] NOMBRES_F = {
            "Sofia", "Camila", "Valentina", "Luciana", "Mariana", "Gabriela", "Daniela", "Fernanda",
            "Antonella", "Carla", "Elena", "Paola", "Natalia", "Andrea", "Victoria", "Micaela",
            "Camila", "Ana", "Lucia", "Esperanza"
    };

    private static final String[] NOMBRES_M = {
            "Mateo", "Santiago", "Diego", "Sebastian", "Adrian", "Nicolas", "Lucas", "Emiliano",
            "Samuel", "Joaquin", "Bruno", "Rodrigo", "Andres", "Tomas", "Mauricio", "Javier",
            "Gustavo", "Fernando", "Carlos", "Miguel"
    };

    private static final String[] APELLIDOS = {
            "Vargas", "Mamani", "Rojas", "Quispe", "Flores", "Gutierrez", "Rivera", "Lopez",
            "Choque", "Mendoza", "Aguilar", "Paz", "Suarez", "Arce", "Cabrera", "Medina",
            "Salazar", "Ortiz", "Romero", "Camacho", "Torrico", "Burgos", "Peinado", "Ramos",
            "Sanchez", "Martinez", "Gonzalez", "Hernandez", "Ramirez", "Torres"
    };

    private static final String[] DIAS_SEMANA = {"LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES"};
    private static final String[] ESTADOS_ASISTENCIA = {"PRESENTE", "AUSENTE", "TARDANZA", "JUSTIFICADO"};
    private static final double[] PESOS_ASISTENCIA = {0.80, 0.08, 0.07, 0.05};

    private static final String[] ESPECIALIDADES = {
            "Matematica", "Lenguaje", "Ciencias Naturales", "Ciencias Sociales", "Educacion Fisica",
            "Musica", "Artes", "Ingles", "Fisica", "Quimica", "Biologia", "Filosofia", "Informatica"
    };

    private static final String[] PARENTESCOS = {
            "Padre", "Madre", "Padre", "Madre", "Tio(a)", "Abuelo(a)", "Hermano(a)", "Otro"
    };

    private static final String[][] CURSOS = {
            {"PRI-1", "1ro Primaria", "Primaria", "6"},
            {"PRI-2", "2do Primaria", "Primaria", "7"},
            {"PRI-3", "3ro Primaria", "Primaria", "8"},
            {"PRI-4", "4to Primaria", "Primaria", "9"},
            {"PRI-5", "5to Primaria", "Primaria", "10"},
            {"PRI-6", "6to Primaria", "Primaria", "11"},
            {"SEC-1", "1ro Secundaria", "Secundaria", "12"},
            {"SEC-2", "2do Secundaria", "Secundaria", "13"},
            {"SEC-3", "3ro Secundaria", "Secundaria", "14"},
            {"SEC-4", "4to Secundaria", "Secundaria", "15"},
            {"SEC-5", "5to Secundaria", "Secundaria", "16"},
            {"SEC-6", "6to Secundaria", "Secundaria", "17"}
    };

    private static final String[] MATERIAS_PRIMARIA = {
            "MAT", "LEN", "CN", "CS", "VER", "APV", "EFD", "MUS"
    };

    private static final String[] MATERIAS_SECUNDARIA = {
            "MAT", "LEN", "CS", "VER", "APV", "EFD", "ING", "TT", "BIO", "FIS", "QUI", "FIL"
    };

    private static final String[][] MATERIAS_DEF = {
            {"MAT", "Matematica", "Ciencias Exactas", "6"},
            {"LEN", "Lenguaje y Comunicacion", "Lenguajes", "5"},
            {"CN", "Ciencias Naturales", "Ciencias", "4"},
            {"CS", "Ciencias Sociales", "Sociedad", "4"},
            {"VER", "Valores y Espiritualidad", "Formacion", "2"},
            {"APV", "Artes Plasticas y Visuales", "Arte", "2"},
            {"EFD", "Educacion Fisica y Deportes", "Salud", "3"},
            {"MUS", "Educacion Musical", "Arte", "2"},
            {"ING", "Lengua Extranjera - Ingles", "Lenguajes", "3"},
            {"TT", "Tecnica y Tecnologia", "Tecnologia", "3"},
            {"BIO", "Biologia", "Ciencias", "4"},
            {"FIS", "Fisica", "Ciencias", "4"},
            {"QUI", "Quimica", "Ciencias", "4"},
            {"FIL", "Filosofia y Psicologia", "Humanidades", "3"}
    };

    private static final String[] TIPOS_EVALUACION = {"PARCIAL", "TRABAJO_PRACTICO"};
    private static final BigDecimal PONDERACION_PARCIAL = new BigDecimal("40");
    private static final BigDecimal PONDERACION_TP = new BigDecimal("60");
    private static final int SEMANAS_ASISTENCIA = 8;
    private static final int HORARIOS_POR_ASIGNACION = 2;

    private final InstitucionRepository institucionRepository;
    private final GestionAcademicaRepository gestionAcademicaRepository;
    private final CursoRepository cursoRepository;
    private final ParaleloRepository paraleloRepository;
    private final MateriaRepository materiaRepository;
    private final AulaRepository aulaRepository;
    private final DocenteRepository docenteRepository;
    private final EstudianteRepository estudianteRepository;
    private final TutorRepository tutorRepository;
    private final EstudianteTutorRepository estudianteTutorRepository;
    private final InscripcionRepository inscripcionRepository;
    private final AsignacionDocenteRepository asignacionDocenteRepository;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final EntityManager entityManager;
    private final ConfiguracionInstitucionRepository configuracionInstitucionRepository;
    private final PeriodoEvaluacionRepository periodoEvaluacionRepository;
    private final EvaluacionMateriaRepository evaluacionMateriaRepository;
    private final ActividadEvaluativaRepository actividadEvaluativaRepository;
    private final CalificacionActividadRepository calificacionActividadRepository;
    private final CalificacionSerRepository calificacionSerRepository;
    private final AutoevaluacionTrimestralRepository autoevaluacionTrimestralRepository;
    private final HorarioClaseRepository horarioClaseRepository;
    private final AsistenciaRegistroRepository asistenciaRegistroRepository;
    private final AsistenciaDetalleRepository asistenciaDetalleRepository;

    @Transactional
    public SeedResult seed() {
        SeedStats stats = new SeedStats();
        List<SeedUser> usuarios = new ArrayList<>();
        Roles roles = loadRoles();

        Institucion institucion = createInstitucion();
        lastInstitucion = institucion;
        stats.incrementCreated("instituciones");
        UUID idInstitucion = institucion.getId();

        entityManager.flush();

        seedSuscripcionActiva(idInstitucion, stats);
        seedConfiguraciones(idInstitucion, stats);
        GestionAcademica gestion = createGestion(idInstitucion);
        stats.incrementCreated("gestiones");

        seedUsuariosAdmin(idInstitucion, roles, usuarios);
        List<Curso> cursos = seedCursos(idInstitucion, stats);
        Map<String, Materia> materias = seedMaterias(idInstitucion, stats);
        List<Docente> docentes = seedDocentes(idInstitucion, roles.docente(), stats, usuarios);
        seedAulas(idInstitucion, stats);
        List<Aula> aulas = aulaRepository.findAllByIdInstitucionOrderByEstadoAscNombreAsc(idInstitucion);
        seedParalelosYCursoMateria(idInstitucion, cursos, gestion, materias, stats);

        int studentIndex = 1;
        for (Curso curso : cursos) {
            for (String paraleloNombre : List.of("A", "B")) {
                Paralelo paralelo = findOrCreateParalelo(idInstitucion, curso.getId(), gestion.getId(), paraleloNombre, 35);
                stats.incrementCreated("paralelos");

                seedAsignaciones(idInstitucion, paralelo, gestion, materias, docentes, stats);
                studentIndex = seedEstudiantes(idInstitucion, curso, paralelo, gestion, roles, stats, usuarios, studentIndex, paraleloNombre);
            }
        }

        log.info("Seed base completado. Creados={}, existentes={}", stats.creados, stats.existentes);
        return new SeedResult("COLEGIO-SAN-MIGUEL", GESTION_NOMBRE, stats.creados, stats.existentes, usuarios);
    }

    // ========================================================================
    // MÉTODOS EXISTENTES (sin cambios)
    // ========================================================================

    private Institucion createInstitucion() {
        return institucionRepository.findByCodigo(INSTITUCION_CODIGO)
                .orElseGet(() -> institucionRepository.save(Institucion.builder()
                        .codigo(INSTITUCION_CODIGO)
                        .nombre(INSTITUCION_NOMBRE)
                        .tipoInstitucion(INSTITUCION_TIPO)
                        .telefono(TELEFONO)
                        .correo("contacto@" + DOMINIO)
                        .direccion(DIRECCION)
                        .build()));
    }

    private void seedSuscripcionActiva(UUID idInstitucion, SeedStats stats) {
        List<UUID> existing = jdbcTemplate.queryForList("""
                SELECT id FROM sia.suscripcion_institucion
                WHERE id_institucion = ? AND estado = 'ACTIVA'
                """, UUID.class, idInstitucion);
        if (!existing.isEmpty()) {
            stats.incrementCreated("suscripciones");
            return;
        }

        List<UUID> planes = jdbcTemplate.queryForList("""
                SELECT id FROM sia.plan_suscripcion
                WHERE codigo = 'PROFESIONAL' AND estado = 'ACTIVO'
                LIMIT 1
                """, UUID.class);
        if (planes.isEmpty()) {
            log.warn("No se pudo crear suscripción para {}: plan PROFESIONAL no encontrado", INSTITUCION_CODIGO);
            return;
        }

        jdbcTemplate.update("""
                INSERT INTO sia.suscripcion_institucion
                (id, id_institucion, id_plan, fecha_inicio, fecha_fin, estado, simulada, observacion)
                VALUES (?, ?, ?, ?, ?, 'ACTIVA', true, ?)
                """,
                UUID.randomUUID(), idInstitucion, planes.getFirst(),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                "Suscripción demo creada por seed Colegio San Miguel");
        stats.incrementCreated("suscripciones");
    }

    private void seedConfiguraciones(UUID idInstitucion, SeedStats stats) {
        for (ConfiguracionCatalog.Definition def : ConfiguracionCatalog.DEFINITIONS) {
            String valor = switch (def.getClave()) {
                case "NOMBRE_CORTO" -> "Colegio San Miguel";
                case "DESCRIPCION" -> INSTITUCION_NOMBRE + " - " + CIUDAD + ", Bolivia";
                case "TELEFONO_CONTACTO" -> TELEFONO;
                case "CORREO_CONTACTO" -> "contacto@" + DOMINIO;
                case "SITIO_WEB" -> "https://www." + DOMINIO;
                case "COLOR_PRIMARIO" -> "#1a5f7a";
                case "MAX_ALUMNOS_AULA" -> "35";
                case "FORMATO_CODIGO_ESTUDIANTE" -> INSTITUCION_CODIGO + "-EST-{SEQ}";
                default -> ConfiguracionCatalog.resolveDefaultValue(def, INSTITUCION_TIPO);
            };
            findOrCreateConfiguracion(idInstitucion, def, valor);
            stats.incrementCreated("configuraciones");
        }
    }

    private GestionAcademica createGestion(UUID idInstitucion) {
        return gestionAcademicaRepository.findByIdInstitucionAndActivaTrue(idInstitucion)
                .orElseGet(() -> gestionAcademicaRepository.save(GestionAcademica.builder()
                        .idInstitucion(idInstitucion)
                        .nombre(GESTION_NOMBRE)
                        .fechaInicio(LocalDate.of(2026, 2, 3))
                        .fechaFin(LocalDate.of(2026, 11, 28))
                        .activa(true)
                        .build()));
    }

    private void seedUsuariosAdmin(UUID idInstitucion, Roles roles, List<SeedUser> usuarios) {
        createUsuario("admin@" + DOMINIO, "Maria", "Rodriguez", idInstitucion, roles.adminInstitucion());
        usuarios.add(new SeedUser("admin@" + DOMINIO, PASSWORD, "ADMIN_INSTITUCION", "Maria Rodriguez", INSTITUCION_NOMBRE));

        createUsuario("director@" + DOMINIO, "Roberto", "Sanchez", idInstitucion, roles.director());
        usuarios.add(new SeedUser("director@" + DOMINIO, PASSWORD, "DIRECTOR", "Roberto Sanchez", INSTITUCION_NOMBRE));

        createUsuario("secretaria@" + DOMINIO, "Carmen", "Torres", idInstitucion, roles.secretario());
        usuarios.add(new SeedUser("secretaria@" + DOMINIO, PASSWORD, "SECRETARIO", "Carmen Torres", INSTITUCION_NOMBRE));

        statsIncrement("usuarios", 3);
    }

    private Usuario createUsuario(String correo, String nombres, String apellidos, UUID idInstitucion, Rol rol) {
        return usuarioRepository.findByCorreo(correo)
                .orElseGet(() -> usuarioRepository.save(Usuario.builder()
                        .idInstitucion(idInstitucion)
                        .correo(correo)
                        .hashContrasena(passwordEncoder.encode(PASSWORD))
                        .nombres(nombres)
                        .apellidos(apellidos)
                        .roles(Set.of(rol))
                        .build()));
    }

    private List<Curso> seedCursos(UUID idInstitucion, SeedStats stats) {
        List<Curso> cursos = new ArrayList<>();
        for (String[] spec : CURSOS) {
            cursos.add(findOrCreateCurso(idInstitucion, spec[0], spec[1], spec[2]));
            stats.incrementCreated("cursos");
        }
        return cursos;
    }

    private Curso findOrCreateCurso(UUID idInstitucion, String codigo, String nombre, String nivel) {
        return cursoRepository.findAllByIdInstitucion(idInstitucion).stream()
                .filter(c -> codigo.equals(c.getCodigo()))
                .findFirst()
                .orElseGet(() -> {
                    Curso c = Curso.builder()
                            .idInstitucion(idInstitucion)
                            .codigo(codigo)
                            .nombre(nombre)
                            .nivel(nivel)
                            .build();
                    Curso saved = cursoRepository.save(c);
                    entityManager.flush();
                    jdbcTemplate.update("UPDATE sia.curso SET orden_visual = ? WHERE id = ?",
                            Integer.parseInt(codigo.substring(4)), saved.getId());
                    return saved;
                });
    }

    private Map<String, Materia> seedMaterias(UUID idInstitucion, SeedStats stats) {
        Map<String, Materia> materias = new LinkedHashMap<>();
        for (String[] spec : MATERIAS_DEF) {
            Materia m = findOrCreateMateria(idInstitucion, spec[0], spec[1], spec[2]);
            materias.put(spec[0], m);
            stats.incrementCreated("materias");
        }
        return materias;
    }

    private Materia findOrCreateMateria(UUID idInstitucion, String codigo, String nombre, String area) {
        return materiaRepository.findAllByIdInstitucion(idInstitucion).stream()
                .filter(m -> codigo.equals(m.getCodigo()))
                .findFirst()
                .orElseGet(() -> materiaRepository.save(Materia.builder()
                        .idInstitucion(idInstitucion)
                        .codigo(codigo)
                        .nombre(nombre)
                        .area(area)
                        .cargaHoraria(4)
                        .build()));
    }

    private List<Docente> seedDocentes(UUID idInstitucion, Rol docenteRol, SeedStats stats, List<SeedUser> usuarios) {
        List<Docente> docentes = new ArrayList<>();
        String[] docentesNombres = {
                "Ana Rojas", "Carlos Mendez", "Patricia Vargas", "Luis Arce", "Ruth Aguilar",
                "Mario Suarez", "Claudia Medina", "Fernando Paz", "Marcela Lopez", "Hugo Salazar",
                "Elena Camacho", "Jorge Rivera", "Marisol Gutierrez", "Oscar Romero", "Diana Centeno",
                "Pablo Iriarte", "Gloria Condori", "Raul Peinado", "Sandra Veizaga", "Alberto Villarroel"
        };

        for (int i = 0; i < docentesNombres.length; i++) {
            String[] partes = docentesNombres[i].split(" ", 2);
            String correo = "docente." + (i + 1) + "@" + DOMINIO;

            Usuario usuario = createUsuario(correo, partes[0], partes[1], idInstitucion, docenteRol);
            usuarios.add(new SeedUser(correo, PASSWORD, "DOCENTE", docentesNombres[i], INSTITUCION_NOMBRE));
            stats.incrementCreated("usuarios");

            String codigoDoc = INSTITUCION_CODIGO + "-DOC-" + String.format("%03d", i + 1);
            String ciDoc = "" + (7000000 + i * 1111);

            Docente docente = findOrCreateDocente(idInstitucion, usuario.getId(), codigoDoc, ciDoc,
                    partes[0], partes[1], "7" + (6000000 + i * 1111), correo, ESPECIALIDADES[i % ESPECIALIDADES.length]);
            docentes.add(docente);
            stats.incrementCreated("docentes");
        }
        return docentes;
    }

    private Docente findOrCreateDocente(UUID idInstitucion, UUID idUsuario, String codigo, String documento,
                                         String nombres, String apellidos, String telefono, String correo, String especialidad) {
        return docenteRepository.findAllByIdInstitucion(idInstitucion).stream()
                .filter(d -> codigo.equals(d.getCodigo()))
                .findFirst()
                .orElseGet(() -> docenteRepository.save(Docente.builder()
                        .idInstitucion(idInstitucion)
                        .idUsuario(idUsuario)
                        .codigo(codigo)
                        .documentoIdentidad(documento)
                        .nombres(nombres)
                        .apellidos(apellidos)
                        .telefono(telefono)
                        .correo(correo)
                        .especialidad(especialidad)
                        .build()));
    }

    private void seedAulas(UUID idInstitucion, SeedStats stats) {
        for (int i = 1; i <= 24; i++) {
            String bloque = i <= 12 ? "Bloque A" : "Bloque B";
            String piso = String.valueOf(((i - 1) / 6) + 1);
            String recursos = i % 3 == 0 ? "Pizarra|Proyector|Internet" : i % 2 == 0 ? "Pizarra|Computadoras" : "Pizarra";

            findOrCreateAula(idInstitucion, INSTITUCION_CODIGO + "-AULA-" + String.format("%03d", i),
                    "Aula " + String.format("%02d", i), 35, bloque + ", Piso " + piso, recursos);
            stats.incrementCreated("aulas");
        }
    }

    private Aula findOrCreateAula(UUID idInstitucion, String codigo, String nombre, int capacidad, String ubicacion, String recursos) {
        return aulaRepository.findAllByIdInstitucionOrderByEstadoAscNombreAsc(idInstitucion).stream()
                .filter(a -> codigo.equals(a.getCodigo()))
                .findFirst()
                .orElseGet(() -> aulaRepository.save(Aula.builder()
                        .idInstitucion(idInstitucion)
                        .codigo(codigo)
                        .nombre(nombre)
                        .capacidad(capacidad)
                        .ubicacion(ubicacion)
                        .recursos(recursos)
                        .build()));
    }

    private void seedParalelosYCursoMateria(UUID idInstitucion, List<Curso> cursos, GestionAcademica gestion,
                                            Map<String, Materia> materias, SeedStats stats) {
        for (Curso curso : cursos) {
            String[] materiasDelCurso = "Secundaria".equals(curso.getNivel()) ? MATERIAS_SECUNDARIA : MATERIAS_PRIMARIA;
            for (String codigo : materiasDelCurso) {
                Materia materia = materias.get(codigo);
                entityManager.flush();
                List<UUID> existing = jdbcTemplate.queryForList("""
                        SELECT id FROM sia.curso_materia
                        WHERE id_institucion = ? AND id_curso = ? AND id_materia = ? AND id_gestion_academica = ?
                        """, UUID.class, idInstitucion, curso.getId(), materia.getId(), gestion.getId());
                if (existing.isEmpty()) {
                    jdbcTemplate.update("""
                            INSERT INTO sia.curso_materia (id, id_institucion, id_curso, id_materia, id_gestion_academica, carga_horaria, estado)
                            VALUES (?, ?, ?, ?, ?, ?, 'ACTIVO')
                            """, UUID.randomUUID(), idInstitucion, curso.getId(), materia.getId(), gestion.getId(), materia.getCargaHoraria());
                }
                stats.incrementCreated("curso_materias");
            }
        }
    }

    private Paralelo findOrCreateParalelo(UUID idInstitucion, UUID idCurso, UUID idGestion, String nombre, int capacidad) {
        return paraleloRepository.findAllByIdInstitucionAndIdCurso(idInstitucion, idCurso).stream()
                .filter(p -> idGestion.equals(p.getIdGestionAcademica()) && nombre.equals(p.getNombre()))
                .findFirst()
                .orElseGet(() -> paraleloRepository.save(Paralelo.builder()
                        .idInstitucion(idInstitucion)
                        .idCurso(idCurso)
                        .idGestionAcademica(idGestion)
                        .nombre(nombre)
                        .capacidad(capacidad)
                        .build()));
    }

    private void seedAsignaciones(UUID idInstitucion, Paralelo paralelo, GestionAcademica gestion,
                                   Map<String, Materia> materias, List<Docente> docentes, SeedStats stats) {
        String nivel = cursoRepository.findAllByIdInstitucion(idInstitucion).stream()
                .filter(c -> c.getId().equals(paralelo.getIdCurso()))
                .findFirst().map(Curso::getNivel).orElse("Primaria");

        String[] materiasDelCurso = "Secundaria".equals(nivel) ? MATERIAS_SECUNDARIA : MATERIAS_PRIMARIA;

        int baseIndex = Math.abs(paralelo.getNombre().hashCode() + paralelo.getIdCurso().hashCode()) % docentes.size();
        for (int i = 0; i < materiasDelCurso.length; i++) {
            Materia materia = materias.get(materiasDelCurso[i]);
            Docente docente = docentes.get((baseIndex + i) % docentes.size());

            boolean exists = asignacionDocenteRepository.existsByIdInstitucionAndIdDocenteAndIdMateriaAndIdParaleloAndIdGestion(
                    idInstitucion, docente.getId(), materia.getId(), paralelo.getId(), gestion.getId());

            if (!exists) {
                asignacionDocenteRepository.save(AsignacionDocente.builder()
                        .idInstitucion(idInstitucion)
                        .idDocente(docente.getId())
                        .idMateria(materia.getId())
                        .idParalelo(paralelo.getId())
                        .idGestion(gestion.getId())
                        .build());
            }
            stats.incrementCreated("asignaciones");
        }
    }

    private int seedEstudiantes(UUID idInstitucion, Curso curso, Paralelo paralelo, GestionAcademica gestion,
                                Roles roles, SeedStats stats, List<SeedUser> usuarios,
                                int startIndex, String paraleloNombre) {
        int next = startIndex;
        int ageBase = "Secundaria".equals(curso.getNivel()) ? 12 : 6;
        int courseNum = Integer.parseInt(curso.getCodigo().substring(4));
        int baseAge = ageBase + courseNum;

        for (int i = 0; i < 20; i++) {
            boolean female = i % 2 == 0;
            String nombre = female ? NOMBRES_F[(next) % NOMBRES_F.length] : NOMBRES_M[(next) % NOMBRES_M.length];
            String apellido1 = APELLIDOS[(next * 3) % APELLIDOS.length];
            String apellido2 = APELLIDOS[(next * 7 + 5) % APELLIDOS.length];
            String nombreCompleto = nombre + " " + apellido1 + " " + apellido2;

            String codigoEst = INSTITUCION_CODIGO + "-EST-" + String.format("%04d", next);
            String documento = String.valueOf(8000000 + next * 111);

            LocalDate fechaNac = LocalDate.of(2026 - baseAge, ((next * 3) % 12) + 1, ((next * 5) % 28) + 1);
            String sexo = female ? "FEMENINO" : "MASCULINO";

            String correoEst = "estudiante." + String.format("%04d", next) + "@" + DOMINIO;
            Usuario usuario = usuarioRepository.findByCorreo(correoEst).orElse(null);
            if (usuario == null) {
                usuario = usuarioRepository.save(Usuario.builder()
                        .idInstitucion(idInstitucion)
                        .correo(correoEst)
                        .hashContrasena(passwordEncoder.encode(PASSWORD))
                        .nombres(nombre)
                        .apellidos(apellido1 + " " + apellido2)
                        .roles(Set.of(roles.estudiante()))
                        .build());
                usuarios.add(new SeedUser(correoEst, PASSWORD, "ESTUDIANTE", nombreCompleto, curso.getNombre() + " " + paraleloNombre));
                stats.incrementCreated("usuarios");
            }

            Estudiante estudiante = findOrCreateEstudiante(idInstitucion, usuario.getId(), codigoEst, documento,
                    nombre, apellido1 + " " + apellido2, fechaNac, sexo,
                    CIUDAD + ", zona " + APELLIDOS[(next * 2) % APELLIDOS.length],
                    "7" + String.format("%07d", 7000000 + next), correoEst);
            stats.incrementCreated("estudiantes");

            String parentesco = PARENTESCOS[(next) % PARENTESCOS.length];
            String nombreTutor = female ? "Maria" : "Jose";
            String apellidoTutor1 = APELLIDOS[(next + 10) % APELLIDOS.length];
            String apellidoTutor2 = APELLIDOS[(next + 15) % APELLIDOS.length];
            String documentoTutor = String.valueOf(9000000 + next * 111);

            Tutor tutor = findOrCreateTutor(idInstitucion, null, documentoTutor, nombreTutor,
                    apellidoTutor1 + " " + apellidoTutor2,
                    "7" + String.format("%07d", 8000000 + next), null,
                    CIUDAD + ", zona " + APELLIDOS[(next + 7) % APELLIDOS.length]);
            stats.incrementCreated("tutores");

            findOrCreateEstudianteTutor(idInstitucion, estudiante.getId(), tutor.getId(), parentesco, true);
            stats.incrementCreated("estudiante_tutores");

            findOrCreateInscripcion(idInstitucion, estudiante.getId(), gestion.getId(), paralelo.getId(), LocalDate.of(2026, 2, 1));
            stats.incrementCreated("inscripciones");

            next++;
        }
        return next;
    }

    private Estudiante findOrCreateEstudiante(UUID idInstitucion, UUID idUsuario, String codigo, String documento,
                                              String nombres, String apellidos, LocalDate fechaNac, String sexo,
                                              String direccion, String telefono, String correo) {
        return estudianteRepository.findAllByIdInstitucion(idInstitucion).stream()
                .filter(e -> codigo.equals(e.getCodigoEstudiante()))
                .findFirst()
                .orElseGet(() -> estudianteRepository.save(Estudiante.builder()
                        .idInstitucion(idInstitucion)
                        .idUsuario(idUsuario)
                        .codigoEstudiante(codigo)
                        .documentoIdentidad(documento)
                        .nombres(nombres)
                        .apellidos(apellidos)
                        .fechaNacimiento(fechaNac)
                        .sexo(sexo)
                        .direccion(direccion)
                        .telefono(telefono)
                        .correo(correo)
                        .build()));
    }

    private Tutor findOrCreateTutor(UUID idInstitucion, UUID idUsuario, String documento, String nombres,
                                     String apellidos, String telefono, String correo, String direccion) {
        return tutorRepository.findAllByIdInstitucion(idInstitucion).stream()
                .filter(t -> documento.equals(t.getDocumentoIdentidad()))
                .findFirst()
                .orElseGet(() -> tutorRepository.save(Tutor.builder()
                        .idInstitucion(idInstitucion)
                        .idUsuario(idUsuario)
                        .documentoIdentidad(documento)
                        .nombres(nombres)
                        .apellidos(apellidos)
                        .telefono(telefono)
                        .correo(correo)
                        .direccion(direccion)
                        .build()));
    }

    private void findOrCreateEstudianteTutor(UUID idInstitucion, UUID idEstudiante, UUID idTutor, String parentesco, boolean principal) {
        estudianteTutorRepository.findByIdInstitucionAndIdEstudianteAndIdTutor(idInstitucion, idEstudiante, idTutor)
                .orElseGet(() -> estudianteTutorRepository.save(EstudianteTutor.builder()
                        .idInstitucion(idInstitucion)
                        .idEstudiante(idEstudiante)
                        .idTutor(idTutor)
                        .parentesco(parentesco)
                        .esPrincipal(principal)
                        .build()));
    }

    private void findOrCreateInscripcion(UUID idInstitucion, UUID idEstudiante, UUID idGestion, UUID idParalelo, LocalDate fecha) {
        boolean exists = inscripcionRepository.findAllByIdInstitucionAndIdEstudiante(idInstitucion, idEstudiante).stream()
                .anyMatch(i -> idGestion.equals(i.getIdGestion()));
        if (!exists) {
            inscripcionRepository.save(Inscripcion.builder()
                    .idInstitucion(idInstitucion)
                    .idEstudiante(idEstudiante)
                    .idGestion(idGestion)
                    .idParalelo(idParalelo)
                    .fechaInscripcion(fecha)
                    .build());
        }
    }

    private ConfiguracionInstitucion findOrCreateConfiguracion(UUID idInstitucion, ConfiguracionCatalog.Definition def, String valor) {
        return configuracionInstitucionRepository.findByIdInstitucionAndClave(idInstitucion, def.getClave())
                .orElseGet(() -> configuracionInstitucionRepository.save(ConfiguracionInstitucion.builder()
                        .idInstitucion(idInstitucion)
                        .clave(def.getClave())
                        .valor(valor)
                        .tipoValor(def.getTipoValor())
                        .descripcion(def.getDescripcion())
                        .build()));
    }

    private Roles loadRoles() {
        return new Roles(
                rol("ADMIN_INSTITUCION"),
                rol("DIRECTOR"),
                rol("SECRETARIO"),
                rol("DOCENTE"),
                rol("ESTUDIANTE"),
                rol("TUTOR")
        );
    }

    private Rol rol(String codigo) {
        return rolRepository.findByCodigo(codigo)
                .orElseThrow(() -> new IllegalStateException("Rol '" + codigo + "' no encontrado"));
    }

    private void statsIncrement(String category, int count) {
    }

    private record Roles(
            Rol adminInstitucion,
            Rol director,
            Rol secretario,
            Rol docente,
            Rol estudiante,
            Rol tutor
    ) {}

    // ========================================================================
    // NUEVOS MÉTODOS: Periodos de evaluación
    // ========================================================================

    private List<PeriodoEvaluacion> seedPeriodosEvaluacion(UUID idInstitucion, GestionAcademica gestion, SeedStats stats) {
        List<PeriodoEvaluacion> periodos = new ArrayList<>();
        LocalDate inicio = gestion.getFechaInicio();
        long diasGestion = gestion.getFechaFin().toEpochDay() - inicio.toEpochDay();
        int tercio = (int) (diasGestion / 3);

        for (int p = 1; p <= 3; p++) {
            int numPeriodo = p;
            if (periodoEvaluacionRepository.findByIdInstitucionAndIdGestionAcademicaAndNumeroPeriodo(
                    idInstitucion, gestion.getId(), numPeriodo).isPresent()) {
                stats.incrementCreated("periodos_evaluacion");
                continue;
            }
            LocalDate pInicio = inicio.plusDays((p - 1) * tercio);
            LocalDate pFin = (p < 3) ? inicio.plusDays(p * tercio - 1) : gestion.getFechaFin();
            PeriodoEvaluacion pe = periodoEvaluacionRepository.save(PeriodoEvaluacion.builder()
                    .idInstitucion(idInstitucion)
                    .idGestionAcademica(gestion.getId())
                    .numeroPeriodo(numPeriodo)
                    .tipoPeriodo("TRIMESTRAL")
                    .fechaInicio(pInicio)
                    .fechaFin(pFin)
                    .estado("ABIERTO")
                    .build());
            periodos.add(pe);
            stats.incrementCreated("periodos_evaluacion");
        }
        return periodos;
    }

    // ========================================================================
    // NUEVOS MÉTODOS: Evaluaciones por materia
    // ========================================================================

    private void seedEvaluacionesMateria(UUID idInstitucion, Map<String, Materia> materias,
                                          List<PeriodoEvaluacion> periodos, SeedStats stats) {
        for (Materia materia : materias.values()) {
            for (PeriodoEvaluacion pe : periodos) {
                for (int t = 0; t < TIPOS_EVALUACION.length; t++) {
                    String tipo = TIPOS_EVALUACION[t];
                    String nombre = tipo + " " + pe.getNumeroPeriodo() + "er Trimestre";
                    BigDecimal ponderacion = (t == 0) ? PONDERACION_PARCIAL : PONDERACION_TP;

                    boolean exists = evaluacionMateriaRepository
                            .existsByIdInstitucionAndIdMateriaAndPeriodoAndNombreIgnoreCase(
                                    idInstitucion, materia.getId(), pe.getNumeroPeriodo(), nombre);
                    if (!exists) {
                        evaluacionMateriaRepository.save(EvaluacionMateria.builder()
                                .idInstitucion(idInstitucion)
                                .idMateria(materia.getId())
                                .periodo(pe.getNumeroPeriodo())
                                .tipo(tipo)
                                .nombre(nombre)
                                .ponderacion(ponderacion)
                                .build());
                    }
                    stats.incrementCreated("evaluaciones_materia");
                }
            }
        }
    }

    // ========================================================================
    // NUEVOS MÉTODOS: Actividades evaluativas + calificaciones
    // ========================================================================

    private void seedActividadesYCalificaciones(UUID idInstitucion, Map<String, Materia> materias,
                                                 List<Docente> docentes, List<PeriodoEvaluacion> periodos,
                                                 List<Inscripcion> inscripciones, List<Estudiante> estudiantes,
                                                 List<Curso> cursos, Random rng, SeedStats stats) {
        Map<UUID, List<Inscripcion>> insPorEstudiante = inscripciones.stream()
                .collect(Collectors.groupingBy(Inscripcion::getIdEstudiante));

        for (Materia materia : materias.values()) {
            Docente docente = docentes.get(Math.abs(materia.getCodigo().hashCode()) % docentes.size());
            for (PeriodoEvaluacion pe : periodos) {
                String[] dimensiones = {"SABER", "HACER"};
                for (int d = 0; d < dimensiones.length; d++) {
                    String nombreAct = dimensiones[d] + " - " + materia.getNombre() + " T" + pe.getNumeroPeriodo();

                    boolean existsAct = false;
                    try {
                        existsAct = actividadEvaluativaRepository
                                .existsByIdInstitucionAndIdPeriodoEvaluacionAndNombreActividadIgnoreCase(
                                        idInstitucion, pe.getId(), nombreAct);
                    } catch (Exception e) {
                        // ignore
                    }
                    if (existsAct) {
                        stats.incrementCreated("actividades_evaluativas");
                        stats.incrementCreated("calificaciones_actividad");
                        continue;
                    }

                    LocalDate fechaAct = pe.getFechaInicio().plusDays(d * 15 + 5);

                    ActividadEvaluativa actividad = actividadEvaluativaRepository.save(
                            ActividadEvaluativa.builder()
                                    .idInstitucion(idInstitucion)
                                    .idPeriodoEvaluacion(pe.getId())
                                    .idMateria(materia.getId())
                                    .idDocente(docente.getId())
                                    .nombreActividad(nombreAct)
                                    .dimension(dimensiones[d])
                                    .fechaActividad(fechaAct)
                                    .puntajeMaximo(100)
                                    .estado("PUBLICADA")
                                    .build());
                    stats.incrementCreated("actividades_evaluativas");

                    for (Estudiante est : estudiantes) {
                        if (calificacionActividadRepository
                                .findByIdActividadAndIdEstudiante(actividad.getId(), est.getId()).isPresent()) {
                            continue;
                        }
                        BigDecimal nota;
                        if (dimensiones[d].equals("SABER")) {
                            nota = BigDecimal.valueOf(30 + rng.nextDouble() * 65);
                        } else {
                            nota = BigDecimal.valueOf(40 + rng.nextDouble() * 55);
                        }
                        nota = nota.setScale(2, java.math.RoundingMode.HALF_UP);

                        calificacionActividadRepository.save(CalificacionActividad.builder()
                                .idInstitucion(idInstitucion)
                                .idActividad(actividad.getId())
                                .idEstudiante(est.getId())
                                .notaObtenida(nota)
                                .estado("REGISTRADA")
                                .build());
                        stats.incrementCreated("calificaciones_actividad");
                    }
                }
            }
        }
    }

    // ========================================================================
    // NUEVOS MÉTODOS: Calificación SER + Autoevaluación
    // ========================================================================

    private void seedCalificacionSerYAuto(UUID idInstitucion, List<Estudiante> estudiantes,
                                           Map<String, Materia> materias, List<PeriodoEvaluacion> periodos,
                                           Random rng, SeedStats stats) {
        for (Estudiante est : estudiantes) {
            for (Materia materia : materias.values()) {
                for (PeriodoEvaluacion pe : periodos) {
                    if (!calificacionSerRepository
                            .findByIdPeriodoEvaluacionAndIdEstudianteAndIdMateria(pe.getId(), est.getId(), materia.getId())
                            .isPresent()) {
                        BigDecimal notaSer = BigDecimal.valueOf(5 + rng.nextDouble() * 5)
                                .setScale(2, java.math.RoundingMode.HALF_UP);
                        calificacionSerRepository.save(CalificacionSer.builder()
                                .idInstitucion(idInstitucion)
                                .idPeriodoEvaluacion(pe.getId())
                                .idEstudiante(est.getId())
                                .idMateria(materia.getId())
                                .notaSer(notaSer)
                                .estado("REGISTRADA")
                                .build());
                        stats.incrementCreated("calificaciones_ser");
                    }

                    if (!autoevaluacionTrimestralRepository
                            .findByIdPeriodoEvaluacionAndIdEstudianteAndIdMateria(pe.getId(), est.getId(), materia.getId())
                            .isPresent()) {
                        BigDecimal notaAuto = BigDecimal.valueOf(2 + rng.nextDouble() * 3)
                                .setScale(2, java.math.RoundingMode.HALF_UP);
                        autoevaluacionTrimestralRepository.save(AutoevaluacionTrimestral.builder()
                                .idInstitucion(idInstitucion)
                                .idPeriodoEvaluacion(pe.getId())
                                .idEstudiante(est.getId())
                                .idMateria(materia.getId())
                                .notaAutoevaluacion(notaAuto)
                                .estado("REGISTRADA")
                                .build());
                        stats.incrementCreated("autoevaluaciones");
                    }
                }
            }
        }
    }

    // ========================================================================
    // NUEVOS MÉTODOS: Horarios
    // ========================================================================

    private void seedHorarios(UUID idInstitucion, List<AsignacionDocente> asignaciones,
                               List<Aula> aulas, SeedStats stats) {
        LocalTime[] horasInicio = {LocalTime.of(8, 0), LocalTime.of(9, 30),
                LocalTime.of(11, 0), LocalTime.of(14, 0), LocalTime.of(15, 30)};
        LocalTime[] horasFin = {LocalTime.of(9, 15), LocalTime.of(10, 45),
                LocalTime.of(12, 15), LocalTime.of(15, 15), LocalTime.of(16, 45)};

        int aulaIdx = 0;
        for (AsignacionDocente asig : asignaciones) {
            List<HorarioClase> existing = horarioClaseRepository.findByIdAsignacionDocenteAndEstado(
                    asig.getId(), "ACTIVO");
            if (!existing.isEmpty()) {
                stats.incrementCreated("horarios");
                continue;
            }

            for (int h = 0; h < HORARIOS_POR_ASIGNACION; h++) {
                Aula aula = aulas.get(aulaIdx % aulas.size());
                aulaIdx++;
                int slot = Math.abs(asig.getIdMateria().hashCode() + h) % horasInicio.length;
                String dia = DIAS_SEMANA[(Math.abs(asig.getId().hashCode()) + h) % DIAS_SEMANA.length];

                horarioClaseRepository.save(HorarioClase.builder()
                        .idInstitucion(idInstitucion)
                        .idAsignacionDocente(asig.getId())
                        .idAula(aula.getId())
                        .diaSemana(dia)
                        .horaInicio(horasInicio[slot])
                        .horaFin(horasFin[slot])
                        .build());
                stats.incrementCreated("horarios");
            }
        }
    }

    // ========================================================================
    // NUEVOS MÉTODOS: Asistencias
    // ========================================================================

    private void seedAsistencias(UUID idInstitucion, List<AsignacionDocente> asignaciones,
                                  Map<UUID, List<Inscripcion>> inscripcionesPorParalelo,
                                  GestionAcademica gestion, Random rng, SeedStats stats) {
        LocalDate inicio = gestion.getFechaInicio().plusWeeks(2);

        for (AsignacionDocente asig : asignaciones) {
            List<Inscripcion> inscritos = inscripcionesPorParalelo
                    .getOrDefault(asig.getIdParalelo(), List.of());
            if (inscritos.isEmpty()) continue;

            for (int s = 0; s < SEMANAS_ASISTENCIA; s++) {
                LocalDate fecha = inicio.plusWeeks(s);

                boolean exists = asistenciaRegistroRepository
                        .existsByIdInstitucionAndIdAsignacionDocenteAndFecha(idInstitucion, asig.getId(), fecha);
                if (exists) {
                    stats.incrementCreated("asistencias_registro");
                    stats.incrementCreated("asistencias_detalle");
                    continue;
                }

                AsistenciaRegistro reg = asistenciaRegistroRepository.save(
                        AsistenciaRegistro.builder()
                                .idInstitucion(idInstitucion)
                                .idAsignacionDocente(asig.getId())
                                .fecha(fecha)
                                .build());
                stats.incrementCreated("asistencias_registro");

                for (Inscripcion ins : inscritos) {
                    double roll = rng.nextDouble();
                    String estado = ESTADOS_ASISTENCIA[0];
                    double acc = 0;
                    for (int i = 0; i < ESTADOS_ASISTENCIA.length; i++) {
                        acc += PESOS_ASISTENCIA[i];
                        if (roll <= acc) {
                            estado = ESTADOS_ASISTENCIA[i];
                            break;
                        }
                    }

                    if (!asistenciaDetalleRepository
                            .existsByIdAsistenciaRegistroAndIdInscripcion(reg.getId(), ins.getId())) {
                        asistenciaDetalleRepository.save(AsistenciaDetalle.builder()
                                .idAsistenciaRegistro(reg.getId())
                                .idInscripcion(ins.getId())
                                .estadoAsistencia(estado)
                                .build());
                        stats.incrementCreated("asistencias_detalle");
                    }
                }
            }
        }
    }

    private Institucion lastInstitucion;

    public Institucion getInstitucion() {
        return lastInstitucion;
    }

    @Transactional
    public void seedForInstitution(UUID idInstitucion, String codigo, String nombre) {
        String dominio = codigo.toLowerCase().replaceAll("[^a-z0-9]", "") + ".edu.bo";
        Roles roles = loadRoles();
        SeedStats stats = new SeedStats();

        seedSuscripcionActiva(idInstitucion, stats);
        seedConfiguraciones(idInstitucion, codigo, nombre, stats);
        GestionAcademica gestion = createGestion(idInstitucion);
        stats.incrementCreated("gestiones");

        List<SeedUser> usuarios = new ArrayList<>();
        seedUsuariosAdmin(idInstitucion, codigo, dominio, roles, usuarios);
        List<Curso> cursos = seedCursos(idInstitucion, stats);
        Map<String, Materia> materias = seedMaterias(idInstitucion, stats);
        List<Docente> docentes = seedDocentes(idInstitucion, codigo, dominio, roles.docente(), stats, usuarios);
        seedAulas(idInstitucion, codigo, stats);
        List<Aula> aulas = aulaRepository.findAllByIdInstitucionOrderByEstadoAscNombreAsc(idInstitucion);
        seedParalelosYCursoMateria(idInstitucion, cursos, gestion, materias, stats);

        int studentIndex = 1;
        for (Curso curso : cursos) {
            for (String paraleloNombre : List.of("A", "B")) {
                Paralelo paralelo = findOrCreateParalelo(idInstitucion, curso.getId(), gestion.getId(), paraleloNombre, 35);
                stats.incrementCreated("paralelos");
                seedAsignaciones(idInstitucion, paralelo, gestion, materias, docentes, stats);
                studentIndex = seedEstudiantes(idInstitucion, curso, paralelo, gestion, roles, stats, usuarios,
                        studentIndex, paraleloNombre, codigo, dominio);
            }
        }
        log.info("Seed completado para {} ({}). Creados={}", nombre, codigo, stats.creados);
    }

    private void seedConfiguraciones(UUID idInstitucion, String codigo, String nombre, SeedStats stats) {
        for (ConfiguracionCatalog.Definition def : ConfiguracionCatalog.DEFINITIONS) {
            String valor = switch (def.getClave()) {
                case "NOMBRE_CORTO" -> nombre;
                case "DESCRIPCION" -> nombre + " - Santa Cruz, Bolivia";
                case "TELEFONO_CONTACTO" -> "33112345";
                case "CORREO_CONTACTO" -> "contacto@" + codigo.toLowerCase().replaceAll("[^a-z0-9]", "") + ".edu.bo";
                case "SITIO_WEB" -> "https://www." + codigo.toLowerCase().replaceAll("[^a-z0-9]", "") + ".edu.bo";
                case "COLOR_PRIMARIO" -> "#1a5f7a";
                case "MAX_ALUMNOS_AULA" -> "35";
                case "FORMATO_CODIGO_ESTUDIANTE" -> codigo + "-EST-{SEQ}";
                default -> ConfiguracionCatalog.resolveDefaultValue(def, "PRIVADO");
            };
            findOrCreateConfiguracion(idInstitucion, def, valor);
            stats.incrementCreated("configuraciones");
        }
    }

    private void seedUsuariosAdmin(UUID idInstitucion, String codigo, String dominio, Roles roles, List<SeedUser> usuarios) {
        String pass = "Colegio2026!";
        createUsuario("admin@" + dominio, "Maria", "Rodriguez", idInstitucion, roles.adminInstitucion());
        createUsuario("director@" + dominio, "Roberto", "Sanchez", idInstitucion, roles.director());
        createUsuario("secretaria@" + dominio, "Carmen", "Torres", idInstitucion, roles.secretario());
        log.info("  Admins creados para {}: admin@{}, director@{}, secretaria@{}", codigo, dominio, dominio, dominio);
    }

    private List<Docente> seedDocentes(UUID idInstitucion, String codigo, String dominio, Rol docenteRol, SeedStats stats, List<SeedUser> usuarios) {
        List<Docente> docentes = new ArrayList<>();
        String[] docentesNombres = {
                "Ana Rojas", "Carlos Mendez", "Patricia Vargas", "Luis Arce", "Ruth Aguilar",
                "Mario Suarez", "Claudia Medina", "Fernando Paz", "Marcela Lopez", "Hugo Salazar",
                "Elena Camacho", "Jorge Rivera", "Marisol Gutierrez", "Oscar Romero", "Diana Centeno",
                "Pablo Iriarte", "Gloria Condori", "Raul Peinado", "Sandra Veizaga", "Alberto Villarroel"
        };
        for (int i = 0; i < docentesNombres.length; i++) {
            String[] partes = docentesNombres[i].split(" ", 2);
            String correo = "docente." + (i + 1) + "@" + dominio;
            Usuario usuario = createUsuario(correo, partes[0], partes[1], idInstitucion, docenteRol);
            String codigoDoc = codigo + "-DOC-" + String.format("%03d", i + 1);
            String ciDoc = "" + (7000000 + i * 1111);
            Docente docente = findOrCreateDocente(idInstitucion, usuario.getId(), codigoDoc, ciDoc,
                    partes[0], partes[1], "7" + (6000000 + i * 1111), correo, ESPECIALIDADES[i % ESPECIALIDADES.length]);
            docentes.add(docente);
            stats.incrementCreated("docentes");
        }
        return docentes;
    }

    private void seedAulas(UUID idInstitucion, String codigo, SeedStats stats) {
        for (int i = 1; i <= 24; i++) {
            String bloque = i <= 12 ? "Bloque A" : "Bloque B";
            String piso = String.valueOf(((i - 1) / 6) + 1);
            String recursos = i % 3 == 0 ? "Pizarra|Proyector|Internet" : i % 2 == 0 ? "Pizarra|Computadoras" : "Pizarra";
            findOrCreateAula(idInstitucion, codigo + "-AULA-" + String.format("%03d", i),
                    "Aula " + String.format("%02d", i), 35, bloque + ", Piso " + piso, recursos);
            stats.incrementCreated("aulas");
        }
    }

    private int seedEstudiantes(UUID idInstitucion, Curso curso, Paralelo paralelo, GestionAcademica gestion,
                                Roles roles, SeedStats stats, List<SeedUser> usuarios,
                                int startIndex, String paraleloNombre, String codigo, String dominio) {
        int next = startIndex;
        int ageBase = "Secundaria".equals(curso.getNivel()) ? 12 : 6;
        int courseNum = Integer.parseInt(curso.getCodigo().substring(4));
        int baseAge = ageBase + courseNum;

        for (int i = 0; i < 20; i++) {
            boolean female = i % 2 == 0;
            String nombre = female ? NOMBRES_F[(next) % NOMBRES_F.length] : NOMBRES_M[(next) % NOMBRES_M.length];
            String apellido1 = APELLIDOS[(next * 3) % APELLIDOS.length];
            String apellido2 = APELLIDOS[(next * 7 + 5) % APELLIDOS.length];
            String nombreCompleto = nombre + " " + apellido1 + " " + apellido2;
            String codigoEst = codigo + "-EST-" + String.format("%04d", next);
            String documento = String.valueOf(8000000 + next * 111);
            LocalDate fechaNac = LocalDate.of(2026 - baseAge, ((next * 3) % 12) + 1, ((next * 5) % 28) + 1);
            String sexo = female ? "FEMENINO" : "MASCULINO";
            String correoEst = "estudiante." + String.format("%04d", next) + "@" + dominio;

            Usuario usuario = usuarioRepository.findByCorreo(correoEst).orElse(null);
            if (usuario == null) {
                usuario = usuarioRepository.save(Usuario.builder()
                        .idInstitucion(idInstitucion)
                        .correo(correoEst)
                        .hashContrasena(passwordEncoder.encode(PASSWORD))
                        .nombres(nombre)
                        .apellidos(apellido1 + " " + apellido2)
                        .roles(Set.of(roles.estudiante()))
                        .build());
                stats.incrementCreated("usuarios");
            }

            Estudiante estudiante = findOrCreateEstudiante(idInstitucion, usuario.getId(), codigoEst, documento,
                    nombre, apellido1 + " " + apellido2, fechaNac, sexo,
                    "Santa Cruz, zona " + APELLIDOS[(next * 2) % APELLIDOS.length],
                    "7" + String.format("%07d", 7000000 + next), correoEst);
            stats.incrementCreated("estudiantes");

            String parentesco = PARENTESCOS[(next) % PARENTESCOS.length];
            String nombreTutor = female ? "Maria" : "Jose";
            String apellidoTutor1 = APELLIDOS[(next + 10) % APELLIDOS.length];
            String apellidoTutor2 = APELLIDOS[(next + 15) % APELLIDOS.length];
            String documentoTutor = String.valueOf(9000000 + next * 111);
            Tutor tutor = findOrCreateTutor(idInstitucion, null, documentoTutor, nombreTutor,
                    apellidoTutor1 + " " + apellidoTutor2,
                    "7" + String.format("%07d", 8000000 + next), null,
                    "Santa Cruz, zona " + APELLIDOS[(next + 7) % APELLIDOS.length]);
            stats.incrementCreated("tutores");
            findOrCreateEstudianteTutor(idInstitucion, estudiante.getId(), tutor.getId(), parentesco, true);
            stats.incrementCreated("estudiante_tutores");
            findOrCreateInscripcion(idInstitucion, estudiante.getId(), gestion.getId(), paralelo.getId(), LocalDate.of(2026, 2, 1));
            stats.incrementCreated("inscripciones");
            next++;
        }
        return next;
    }

    private static class SeedStats {
        private final Map<String, Integer> creados = new LinkedHashMap<>();
        private final Map<String, Integer> existentes = new LinkedHashMap<>();

        private void incrementCreated(String category) {
            creados.merge(category, 1, Integer::sum);
        }

        private Map<String, Integer> creados() { return creados; }
        private Map<String, Integer> existentes() { return existentes; }
    }
}
