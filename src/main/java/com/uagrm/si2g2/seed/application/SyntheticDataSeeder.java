package com.uagrm.si2g2.seed.application;

import com.uagrm.si2g2.academico.domain.GestionAcademica;
import com.uagrm.si2g2.academico.domain.GestionAcademicaRepository;
import com.uagrm.si2g2.asignacion.domain.AsignacionDocente;
import com.uagrm.si2g2.asignacion.domain.AsignacionDocenteRepository;
import com.uagrm.si2g2.aula.domain.Aula;
import com.uagrm.si2g2.aula.domain.AulaRepository;
import com.uagrm.si2g2.auth.domain.Rol;
import com.uagrm.si2g2.auth.domain.RolRepository;
import com.uagrm.si2g2.auth.domain.Usuario;
import com.uagrm.si2g2.auth.domain.UsuarioRepository;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyntheticDataSeeder {

    private static final String PASSWORD = "Demo12345!";
    private static final String GESTION_NOMBRE = "Gestion Academica 2026";

    private static final SchoolSpec[] SCHOOLS = {
            new SchoolSpec("UEM-001", "Unidad Educativa Modelo", "PRIVADO",
                    "Santa Cruz", "Av. Banzer entre 3er y 4to anillo", "modelo.edu.bo",
                    List.of("A", "B"), 10, 32),
            new SchoolSpec("UEC-002", "Unidad Educativa Cristo Rey de Convenio", "CONVENIO",
                    "Cochabamba", "Zona Cala Cala, calle Libertad 245", "cristorey.edu.bo",
                    List.of("A"), 8, 30)
    };

    private static final CourseSpec[] COURSES = {
            new CourseSpec("PRI-1", "1ro Primaria", "Primaria", 1, 6),
            new CourseSpec("PRI-2", "2do Primaria", "Primaria", 2, 7),
            new CourseSpec("PRI-3", "3ro Primaria", "Primaria", 3, 8),
            new CourseSpec("PRI-4", "4to Primaria", "Primaria", 4, 9),
            new CourseSpec("PRI-5", "5to Primaria", "Primaria", 5, 10),
            new CourseSpec("PRI-6", "6to Primaria", "Primaria", 6, 11),
            new CourseSpec("SEC-1", "1ro Secundaria", "Secundaria", 7, 12),
            new CourseSpec("SEC-2", "2do Secundaria", "Secundaria", 8, 13),
            new CourseSpec("SEC-3", "3ro Secundaria", "Secundaria", 9, 14),
            new CourseSpec("SEC-4", "4to Secundaria", "Secundaria", 10, 15),
            new CourseSpec("SEC-5", "5to Secundaria", "Secundaria", 11, 16),
            new CourseSpec("SEC-6", "6to Secundaria", "Secundaria", 12, 17)
    };

    private static final SubjectSpec[] SUBJECTS = {
            new SubjectSpec("MAT", "Matematica", "Ciencias Exactas", 6),
            new SubjectSpec("LEN", "Comunicacion y Lenguajes", "Lenguajes", 5),
            new SubjectSpec("CN", "Ciencias Naturales", "Ciencias", 4),
            new SubjectSpec("CS", "Ciencias Sociales", "Sociedad", 4),
            new SubjectSpec("VER", "Valores, Espiritualidad y Religiones", "Cosmovisiones", 2),
            new SubjectSpec("APV", "Artes Plasticas y Visuales", "Arte", 2),
            new SubjectSpec("EFD", "Educacion Fisica y Deportes", "Salud", 2),
            new SubjectSpec("MUS", "Educacion Musical", "Arte", 2),
            new SubjectSpec("ING", "Lengua Extranjera Ingles", "Lenguajes", 3),
            new SubjectSpec("TT", "Tecnica Tecnologica General", "Tecnologia", 3),
            new SubjectSpec("BIO", "Biologia", "Ciencias", 4),
            new SubjectSpec("FIS", "Fisica", "Ciencias", 4),
            new SubjectSpec("QUI", "Quimica", "Ciencias", 4),
            new SubjectSpec("FIL", "Filosofia y Psicologia", "Humanidades", 3)
    };

    private static final String[] NOMBRES_F = {
            "Sofia", "Camila", "Valeria", "Lucia", "Mariana", "Gabriela", "Daniela", "Fernanda",
            "Antonella", "Carla", "Elena", "Paola", "Natalia", "Andrea", "Victoria", "Micaela"
    };

    private static final String[] NOMBRES_M = {
            "Mateo", "Santiago", "Diego", "Sebastian", "Adrian", "Nicolas", "Lucas", "Emiliano",
            "Samuel", "Joaquin", "Bruno", "Rodrigo", "Andres", "Tomas", "Mauricio", "Javier"
    };

    private static final String[] APELLIDOS = {
            "Vargas", "Mamani", "Rojas", "Quispe", "Flores", "Gutierrez", "Rivera", "Lopez",
            "Choque", "Mendoza", "Aguilar", "Paz", "Suarez", "Arce", "Cabrera", "Medina",
            "Salazar", "Ortiz", "Romero", "Camacho"
    };

    private static final String[] DOCENTES = {
            "Ana Rojas", "Carlos Mendez", "Patricia Vargas", "Luis Arce", "Ruth Aguilar", "Mario Suarez",
            "Claudia Medina", "Fernando Paz", "Marcela Lopez", "Hugo Salazar", "Elena Camacho", "Jorge Rivera",
            "Marisol Gutierrez", "Oscar Romero"
    };

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

    @Transactional
    public SeedResult seed() {
        SeedStats stats = new SeedStats();
        List<SeedUser> usuarios = new ArrayList<>();
        Roles roles = loadRoles();

        for (SchoolSpec school : SCHOOLS) {
            seedSchool(school, roles, stats, usuarios);
        }

        log.info("Seed sintetico verificado. Creados={}, existentes={}", stats.creados(), stats.existentes());
        return new SeedResult("MULTI-COLEGIO", GESTION_NOMBRE, stats.creados(), stats.existentes(), usuarios);
    }

    private void seedSchool(SchoolSpec school, Roles roles, SeedStats stats, List<SeedUser> usuarios) {
        Institucion institucion = mark(stats, "instituciones", findOrCreateInstitucion(school));
        UUID idInstitucion = institucion.getId();
        seedConfiguraciones(school, idInstitucion, stats);
        GestionAcademica gestion = mark(stats, "gestiones", findOrCreateGestion(idInstitucion));

        Usuario admin = mark(stats, "usuarios", findOrCreateUsuario(
                "admin@" + school.domain(), "Admin", shortName(school.name()), idInstitucion, roles.adminInstitucion()));
        Usuario director = mark(stats, "usuarios", findOrCreateUsuario(
                "director@" + school.domain(), "Daniel", "Quiroga", idInstitucion, roles.director()));
        Usuario secretario = mark(stats, "usuarios", findOrCreateUsuario(
                "secretaria@" + school.domain(), "Mariela", "Ribera", idInstitucion, roles.secretario()));

        usuarios.add(user(admin.getCorreo(), "ADMIN_INSTITUCION", admin.getNombres() + " " + admin.getApellidos(), school.name()));
        usuarios.add(user(director.getCorreo(), "DIRECTOR", director.getNombres() + " " + director.getApellidos(), school.name()));
        usuarios.add(user(secretario.getCorreo(), "SECRETARIO", secretario.getNombres() + " " + secretario.getApellidos(), school.name()));

        List<Curso> cursos = seedCursos(idInstitucion, stats);
        Map<String, Materia> materias = seedMaterias(idInstitucion, stats);
        List<Docente> docentes = seedDocentes(school, idInstitucion, roles.docente(), stats, usuarios);
        seedAulas(school, idInstitucion, stats);

        int parallelIndex = 0;
        int studentIndex = 1;
        for (Curso curso : cursos) {
            for (String paraleloNombre : school.parallels()) {
                parallelIndex++;
                Paralelo paralelo = mark(stats, "paralelos", findOrCreateParalelo(
                        idInstitucion, curso.getId(), gestion.getId(), paraleloNombre, school.parallelCapacity()));
                seedCursoMaterias(idInstitucion, curso, gestion, materias, stats);
                seedAsignaciones(idInstitucion, curso, paralelo, gestion, materias, docentes, stats);
                studentIndex = seedStudentsForParallel(school, idInstitucion, curso, paralelo, gestion, roles, stats, usuarios, studentIndex, parallelIndex);
            }
        }
    }

    private List<Curso> seedCursos(UUID idInstitucion, SeedStats stats) {
        List<Curso> cursos = new ArrayList<>();
        for (CourseSpec spec : COURSES) {
            cursos.add(mark(stats, "cursos", findOrCreateCurso(idInstitucion, spec)));
        }
        return cursos;
    }

    private Map<String, Materia> seedMaterias(UUID idInstitucion, SeedStats stats) {
        Map<String, Materia> materias = new LinkedHashMap<>();
        for (SubjectSpec spec : SUBJECTS) {
            materias.put(spec.code(), mark(stats, "materias", findOrCreateMateria(idInstitucion, spec)));
        }
        return materias;
    }

    private List<Docente> seedDocentes(SchoolSpec school, UUID idInstitucion, Rol docenteRol, SeedStats stats, List<SeedUser> usuarios) {
        List<Docente> docentes = new ArrayList<>();
        for (int i = 0; i < DOCENTES.length; i++) {
            String[] parts = DOCENTES[i].split(" ", 2);
            String correo = "docente." + (i + 1) + "@" + school.domain();
            Usuario usuario = mark(stats, "usuarios", findOrCreateUsuario(correo, parts[0], parts[1], idInstitucion, docenteRol));
            usuarios.add(user(correo, "DOCENTE", DOCENTES[i], school.name()));
            docentes.add(mark(stats, "docentes", findOrCreateDocente(
                    idInstitucion,
                    usuario.getId(),
                    school.code() + "-DOC-" + pad(i + 1, 3),
                    docNumber(school, 20_000 + i),
                    parts[0],
                    parts[1],
                    "7" + pad(600_000 + i, 7),
                    correo,
                    SUBJECTS[i % SUBJECTS.length].name()
            )));
        }
        return docentes;
    }

    private void seedAulas(SchoolSpec school, UUID idInstitucion, SeedStats stats) {
        int total = Math.max(COURSES.length * school.parallels().size(), 12);
        for (int i = 1; i <= total; i++) {
            String bloque = i <= total / 2 ? "Bloque A" : "Bloque B";
            String recursos = i % 4 == 0 ? "Pizarra|Proyector|Internet"
                    : i % 3 == 0 ? "Pizarra|Computadoras"
                    : "Pizarra";
            mark(stats, "aulas", findOrCreateAula(
                    idInstitucion,
                    school.code() + "-AULA-" + pad(i, 3),
                    "Aula " + pad(i, 2),
                    school.parallelCapacity(),
                    bloque + ", piso " + (((i - 1) / 6) + 1),
                    recursos
            ));
        }
    }

    private void seedCursoMaterias(UUID idInstitucion, Curso curso, GestionAcademica gestion, Map<String, Materia> materias, SeedStats stats) {
        for (String code : subjectCodesFor(curso.getNivel())) {
            Materia materia = materias.get(code);
            mark(stats, "curso_materias", findOrCreateCursoMateria(
                    idInstitucion, curso.getId(), materia.getId(), gestion.getId(), materia.getCargaHoraria()));
        }
    }

    private void seedAsignaciones(UUID idInstitucion, Curso curso, Paralelo paralelo, GestionAcademica gestion,
                                  Map<String, Materia> materias, List<Docente> docentes, SeedStats stats) {
        int base = Math.abs(curso.getCodigo().hashCode() + paralelo.getNombre().hashCode());
        int index = 0;
        for (String code : subjectCodesFor(curso.getNivel())) {
            Materia materia = materias.get(code);
            Docente docente = docentes.get((base + index) % docentes.size());
            mark(stats, "asignaciones", findOrCreateAsignacion(
                    idInstitucion, docente.getId(), materia.getId(), paralelo.getId(), gestion.getId()));
            index++;
        }
    }

    private int seedStudentsForParallel(SchoolSpec school, UUID idInstitucion, Curso curso, Paralelo paralelo,
                                        GestionAcademica gestion, Roles roles, SeedStats stats,
                                        List<SeedUser> usuarios, int startIndex, int parallelIndex) {
        int next = startIndex;
        for (int i = 0; i < school.studentsPerParallel(); i++) {
            boolean female = (next + i) % 2 == 0;
            String nombre = female ? NOMBRES_F[(next + i) % NOMBRES_F.length] : NOMBRES_M[(next + i) % NOMBRES_M.length];
            String apellido = APELLIDOS[(next + i) % APELLIDOS.length] + " " + APELLIDOS[(next + i + 5) % APELLIDOS.length];
            String codigo = school.code() + "-EST-" + pad(next, 4);
            String documento = docNumber(school, 40_000 + next);
            String sexo = female ? "FEMENINO" : "MASCULINO";
            LocalDate birthDate = LocalDate.of(2026 - courseAge(curso), ((next + i) % 12) + 1, ((next + i) % 24) + 1);

            Usuario usuario = null;
            if (i < 2 && parallelIndex <= 4) {
                String correo = "estudiante." + pad(next, 4) + "@" + school.domain();
                usuario = mark(stats, "usuarios", findOrCreateUsuario(correo, nombre, apellido, idInstitucion, roles.estudiante()));
                usuarios.add(user(correo, "ESTUDIANTE", nombre + " " + apellido, curso.getNombre() + " " + paralelo.getNombre()));
            }

            Estudiante estudiante = mark(stats, "estudiantes", findOrCreateEstudiante(
                    idInstitucion,
                    usuario == null ? null : usuario.getId(),
                    codigo,
                    documento,
                    nombre,
                    apellido,
                    birthDate,
                    sexo,
                    school.city() + ", zona " + APELLIDOS[(next + 3) % APELLIDOS.length],
                    "7" + pad(700_000 + next, 7),
                    usuario == null ? null : usuario.getCorreo()
            ));

            Tutor tutor = mark(stats, "tutores", findOrCreateTutor(
                    idInstitucion,
                    null,
                    docNumber(school, 80_000 + next),
                    female ? "Maria" : "Jorge",
                    APELLIDOS[(next + 8) % APELLIDOS.length],
                    "7" + pad(800_000 + next, 7),
                    null,
                    school.city() + ", zona " + APELLIDOS[(next + 4) % APELLIDOS.length]
            ));
            mark(stats, "estudiante_tutores", findOrCreateEstudianteTutor(
                    idInstitucion, estudiante.getId(), tutor.getId(), female ? "Madre" : "Padre", true));
            mark(stats, "inscripciones", findOrCreateInscripcion(
                    idInstitucion, estudiante.getId(), gestion.getId(), paralelo.getId(), LocalDate.of(2026, 2, 3 + (i % 10))));
            next++;
        }
        return next;
    }

    private String[] subjectCodesFor(String nivel) {
        if ("Secundaria".equals(nivel)) {
            return new String[]{"MAT", "LEN", "CS", "VER", "APV", "EFD", "ING", "TT", "BIO", "FIS", "QUI", "FIL"};
        }
        return new String[]{"MAT", "LEN", "CN", "CS", "VER", "APV", "EFD", "MUS"};
    }

    private int courseAge(Curso curso) {
        String codigo = curso.getCodigo();
        if (codigo.startsWith("PRI-")) {
            return 5 + Integer.parseInt(codigo.substring(4));
        }
        return 11 + Integer.parseInt(codigo.substring(4));
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
                .orElseThrow(() -> new IllegalStateException("Rol '" + codigo + "' no encontrado. Ejecute db-script.sql primero."));
    }

    private SeedEntity<Institucion> findOrCreateInstitucion(SchoolSpec school) {
        return findOrCreate(
                () -> institucionRepository.findByCodigo(school.code()).orElse(null),
                () -> institucionRepository.save(Institucion.builder()
                        .codigo(school.code())
                        .nombre(school.name())
                        .tipoInstitucion(school.type())
                        .telefono("7" + pad(Math.abs(school.code().hashCode()) % 1_000_000, 7))
                        .correo("contacto@" + school.domain())
                        .direccion(school.address() + ", " + school.city() + " - Bolivia")
                        .build())
        );
    }

    private void seedConfiguraciones(SchoolSpec school, UUID idInstitucion, SeedStats stats) {
        for (ConfiguracionCatalog.Definition definition : ConfiguracionCatalog.DEFINITIONS) {
            String value = configValue(school, definition);
            mark(stats, "configuraciones", findOrCreateConfiguracion(idInstitucion, definition, value));
        }
    }

    private String configValue(SchoolSpec school, ConfiguracionCatalog.Definition definition) {
        return switch (definition.getClave()) {
            case "NOMBRE_CORTO" -> school.shortName();
            case "DESCRIPCION" -> school.name() + " - " + school.city() + ", Bolivia";
            case "TELEFONO_CONTACTO" -> "7" + pad(Math.abs(school.code().hashCode()) % 1_000_000, 7);
            case "CORREO_CONTACTO" -> "contacto@" + school.domain();
            case "SITIO_WEB" -> "https://www." + school.domain();
            case "COLOR_PRIMARIO" -> "CONVENIO".equals(school.type()) ? "#1d4ed8" : "#0f766e";
            case "MAX_ALUMNOS_AULA" -> String.valueOf(school.parallelCapacity());
            case "FORMATO_CODIGO_ESTUDIANTE" -> school.code() + "-EST-{SEQ}";
            default -> ConfiguracionCatalog.resolveDefaultValue(definition, school.type());
        };
    }

    private SeedEntity<ConfiguracionInstitucion> findOrCreateConfiguracion(
            UUID idInstitucion, ConfiguracionCatalog.Definition definition, String value) {
        return findOrCreate(
                () -> configuracionInstitucionRepository
                        .findByIdInstitucionAndClave(idInstitucion, definition.getClave())
                        .orElse(null),
                () -> configuracionInstitucionRepository.save(ConfiguracionInstitucion.builder()
                        .idInstitucion(idInstitucion)
                        .clave(definition.getClave())
                        .valor(value)
                        .tipoValor(definition.getTipoValor())
                        .descripcion(definition.getDescripcion())
                        .build())
        );
    }

    private SeedEntity<GestionAcademica> findOrCreateGestion(UUID idInstitucion) {
        GestionAcademica active = gestionAcademicaRepository.findByIdInstitucionAndActivaTrue(idInstitucion).orElse(null);
        if (active != null) {
            return new SeedEntity<>(active, false);
        }
        return findOrCreate(
                () -> gestionAcademicaRepository.findAllByIdInstitucion(idInstitucion).stream()
                        .filter(g -> GESTION_NOMBRE.equals(g.getNombre()))
                        .findFirst()
                        .orElse(null),
                () -> gestionAcademicaRepository.save(GestionAcademica.builder()
                        .idInstitucion(idInstitucion)
                        .nombre(GESTION_NOMBRE)
                        .fechaInicio(LocalDate.of(2026, 2, 1))
                        .fechaFin(LocalDate.of(2026, 11, 30))
                        .activa(true)
                        .build())
        );
    }

    private SeedEntity<Usuario> findOrCreateUsuario(String correo, String nombres, String apellidos, UUID idInstitucion, Rol rol) {
        return findOrCreate(
                () -> usuarioRepository.findByCorreo(correo).orElse(null),
                () -> usuarioRepository.save(Usuario.builder()
                        .idInstitucion(idInstitucion)
                        .correo(correo)
                        .hashContrasena(passwordEncoder.encode(PASSWORD))
                        .nombres(nombres)
                        .apellidos(apellidos)
                        .roles(Set.of(rol))
                        .build())
        );
    }

    private SeedEntity<Curso> findOrCreateCurso(UUID idInstitucion, CourseSpec spec) {
        return findOrCreate(
                () -> cursoRepository.findAllByIdInstitucion(idInstitucion).stream()
                        .filter(c -> spec.code().equals(c.getCodigo()))
                        .findFirst()
                        .orElse(null),
                () -> {
                    Curso curso = Curso.builder()
                            .idInstitucion(idInstitucion)
                            .codigo(spec.code())
                            .nombre(spec.name())
                            .nivel(spec.level())
                            .build();
                    Curso saved = cursoRepository.save(curso);
                    entityManager.flush();
                    jdbcTemplate.update("UPDATE sia.curso SET orden_visual = ? WHERE id = ?", spec.order(), saved.getId());
                    return saved;
                }
        );
    }

    private SeedEntity<Paralelo> findOrCreateParalelo(UUID idInstitucion, UUID idCurso, UUID idGestion, String nombre, int capacidad) {
        return findOrCreate(
                () -> paraleloRepository.findAllByIdInstitucionAndIdCurso(idInstitucion, idCurso).stream()
                        .filter(p -> idGestion.equals(p.getIdGestionAcademica()) && nombre.equals(p.getNombre()))
                        .findFirst()
                        .orElse(null),
                () -> paraleloRepository.save(Paralelo.builder()
                        .idInstitucion(idInstitucion)
                        .idCurso(idCurso)
                        .idGestionAcademica(idGestion)
                        .nombre(nombre)
                        .capacidad(capacidad)
                        .build())
        );
    }

    private SeedEntity<Materia> findOrCreateMateria(UUID idInstitucion, SubjectSpec spec) {
        return findOrCreate(
                () -> materiaRepository.findAllByIdInstitucion(idInstitucion).stream()
                        .filter(m -> spec.code().equals(m.getCodigo()))
                        .findFirst()
                        .orElse(null),
                () -> materiaRepository.save(Materia.builder()
                        .idInstitucion(idInstitucion)
                        .codigo(spec.code())
                        .nombre(spec.name())
                        .area(spec.area())
                        .cargaHoraria(spec.hours())
                        .build())
        );
    }

    private SeedEntity<Aula> findOrCreateAula(UUID idInstitucion, String codigo, String nombre, int capacidad, String ubicacion, String recursos) {
        return findOrCreate(
                () -> aulaRepository.findAllByIdInstitucionOrderByEstadoAscNombreAsc(idInstitucion).stream()
                        .filter(a -> codigo.equals(a.getCodigo()))
                        .findFirst()
                        .orElse(null),
                () -> aulaRepository.save(Aula.builder()
                        .idInstitucion(idInstitucion)
                        .codigo(codigo)
                        .nombre(nombre)
                        .capacidad(capacidad)
                        .ubicacion(ubicacion)
                        .recursos(recursos)
                        .build())
        );
    }

    private SeedEntity<UUID> findOrCreateCursoMateria(UUID idInstitucion, UUID idCurso, UUID idMateria, UUID idGestion, int cargaHoraria) {
        entityManager.flush();
        List<UUID> existing = jdbcTemplate.queryForList("""
                        SELECT id
                        FROM sia.curso_materia
                        WHERE id_institucion = ? AND id_curso = ? AND id_materia = ? AND id_gestion_academica = ?
                        """,
                UUID.class, idInstitucion, idCurso, idMateria, idGestion);
        if (!existing.isEmpty()) {
            return new SeedEntity<>(existing.getFirst(), false);
        }

        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                        INSERT INTO sia.curso_materia
                        (id, id_institucion, id_curso, id_materia, id_gestion_academica, carga_horaria, estado)
                        VALUES (?, ?, ?, ?, ?, ?, 'ACTIVO')
                        """,
                id, idInstitucion, idCurso, idMateria, idGestion, cargaHoraria);
        return new SeedEntity<>(id, true);
    }

    private SeedEntity<Docente> findOrCreateDocente(
            UUID idInstitucion, UUID idUsuario, String codigo, String documentoIdentidad, String nombres,
            String apellidos, String telefono, String correo, String especialidad) {
        return findOrCreate(
                () -> docenteRepository.findAllByIdInstitucion(idInstitucion).stream()
                        .filter(d -> codigo.equals(d.getCodigo()) || documentoIdentidad.equals(d.getDocumentoIdentidad()))
                        .findFirst()
                        .orElse(null),
                () -> docenteRepository.save(Docente.builder()
                        .idInstitucion(idInstitucion)
                        .idUsuario(idUsuario)
                        .codigo(codigo)
                        .documentoIdentidad(documentoIdentidad)
                        .nombres(nombres)
                        .apellidos(apellidos)
                        .telefono(telefono)
                        .correo(correo)
                        .especialidad(especialidad)
                        .build())
        );
    }

    private SeedEntity<Estudiante> findOrCreateEstudiante(
            UUID idInstitucion, UUID idUsuario, String codigo, String documentoIdentidad, String nombres,
            String apellidos, LocalDate fechaNacimiento, String sexo, String direccion, String telefono, String correo) {
        return findOrCreate(
                () -> estudianteRepository.findAllByIdInstitucion(idInstitucion).stream()
                        .filter(e -> codigo.equals(e.getCodigoEstudiante()) || documentoIdentidad.equals(e.getDocumentoIdentidad()))
                        .findFirst()
                        .orElse(null),
                () -> estudianteRepository.save(Estudiante.builder()
                        .idInstitucion(idInstitucion)
                        .idUsuario(idUsuario)
                        .codigoEstudiante(codigo)
                        .documentoIdentidad(documentoIdentidad)
                        .nombres(nombres)
                        .apellidos(apellidos)
                        .fechaNacimiento(fechaNacimiento)
                        .sexo(sexo)
                        .direccion(direccion)
                        .telefono(telefono)
                        .correo(correo)
                        .build())
        );
    }

    private SeedEntity<Tutor> findOrCreateTutor(
            UUID idInstitucion, UUID idUsuario, String documentoIdentidad, String nombres, String apellidos,
            String telefono, String correo, String direccion) {
        return findOrCreate(
                () -> tutorRepository.findAllByIdInstitucion(idInstitucion).stream()
                        .filter(t -> documentoIdentidad.equals(t.getDocumentoIdentidad()))
                        .findFirst()
                        .orElse(null),
                () -> tutorRepository.save(Tutor.builder()
                        .idInstitucion(idInstitucion)
                        .idUsuario(idUsuario)
                        .documentoIdentidad(documentoIdentidad)
                        .nombres(nombres)
                        .apellidos(apellidos)
                        .telefono(telefono)
                        .correo(correo)
                        .direccion(direccion)
                        .build())
        );
    }

    private SeedEntity<EstudianteTutor> findOrCreateEstudianteTutor(
            UUID idInstitucion, UUID idEstudiante, UUID idTutor, String parentesco, boolean esPrincipal) {
        return findOrCreate(
                () -> estudianteTutorRepository.findByIdInstitucionAndIdEstudianteAndIdTutor(idInstitucion, idEstudiante, idTutor)
                        .orElse(null),
                () -> estudianteTutorRepository.save(EstudianteTutor.builder()
                        .idInstitucion(idInstitucion)
                        .idEstudiante(idEstudiante)
                        .idTutor(idTutor)
                        .parentesco(parentesco)
                        .esPrincipal(esPrincipal)
                        .build())
        );
    }

    private SeedEntity<Inscripcion> findOrCreateInscripcion(
            UUID idInstitucion, UUID idEstudiante, UUID idGestion, UUID idParalelo, LocalDate fechaInscripcion) {
        return findOrCreate(
                () -> inscripcionRepository.findAllByIdInstitucionAndIdEstudiante(idInstitucion, idEstudiante).stream()
                        .filter(i -> idGestion.equals(i.getIdGestion()))
                        .findFirst()
                        .orElse(null),
                () -> inscripcionRepository.save(Inscripcion.builder()
                        .idInstitucion(idInstitucion)
                        .idEstudiante(idEstudiante)
                        .idGestion(idGestion)
                        .idParalelo(idParalelo)
                        .fechaInscripcion(fechaInscripcion)
                        .build())
        );
    }

    private SeedEntity<AsignacionDocente> findOrCreateAsignacion(
            UUID idInstitucion, UUID idDocente, UUID idMateria, UUID idParalelo, UUID idGestion) {
        return findOrCreate(
                () -> asignacionDocenteRepository.existsByIdInstitucionAndIdDocenteAndIdMateriaAndIdParaleloAndIdGestion(
                        idInstitucion, idDocente, idMateria, idParalelo, idGestion)
                        ? asignacionDocenteRepository.findAllByIdInstitucionAndIdDocente(idInstitucion, idDocente).stream()
                                .filter(a -> idMateria.equals(a.getIdMateria())
                                        && idParalelo.equals(a.getIdParalelo())
                                        && idGestion.equals(a.getIdGestion()))
                                .findFirst()
                                .orElse(null)
                        : null,
                () -> asignacionDocenteRepository.save(AsignacionDocente.builder()
                        .idInstitucion(idInstitucion)
                        .idDocente(idDocente)
                        .idMateria(idMateria)
                        .idParalelo(idParalelo)
                        .idGestion(idGestion)
                        .build())
        );
    }

    private <T> SeedEntity<T> findOrCreate(Supplier<T> finder, Supplier<T> creator) {
        T existing = finder.get();
        if (existing != null) {
            return new SeedEntity<>(existing, false);
        }
        return new SeedEntity<>(creator.get(), true);
    }

    private <T> T mark(SeedStats stats, String categoria, SeedEntity<T> seedEntity) {
        stats.increment(seedEntity.created() ? stats.creados() : stats.existentes(), categoria);
        return seedEntity.value();
    }

    private SeedUser user(String correo, String rol, String nombre, String referencia) {
        return new SeedUser(correo, PASSWORD, rol, nombre, referencia);
    }

    private String shortName(String name) {
        String[] words = name.split(" ");
        return words.length <= 2 ? name : words[words.length - 2] + " " + words[words.length - 1];
    }

    private String pad(int value, int digits) {
        return String.format("%0" + digits + "d", value);
    }

    private String docNumber(SchoolSpec school, int base) {
        int prefix = Math.abs(school.code().hashCode()) % 100;
        return prefix + pad(base, 6);
    }

    private record Roles(
            Rol adminInstitucion,
            Rol director,
            Rol secretario,
            Rol docente,
            Rol estudiante,
            Rol tutor
    ) {
    }

    private record SchoolSpec(
            String code,
            String name,
            String type,
            String city,
            String address,
            String domain,
            List<String> parallels,
            int studentsPerParallel,
            int parallelCapacity
    ) {
        private String shortName() {
            return name.contains("Cristo Rey") ? "Cristo Rey" : "U.E. Modelo";
        }
    }

    private record CourseSpec(String code, String name, String level, int order, int expectedAge) {
    }

    private record SubjectSpec(String code, String name, String area, int hours) {
    }

    private record SeedEntity<T>(T value, boolean created) {
    }

    private static class SeedStats {
        private final Map<String, Integer> creados = new LinkedHashMap<>();
        private final Map<String, Integer> existentes = new LinkedHashMap<>();

        private void increment(Map<String, Integer> target, String categoria) {
            target.merge(categoria, 1, Integer::sum);
        }

        private Map<String, Integer> creados() {
            return creados;
        }

        private Map<String, Integer> existentes() {
            return existentes;
        }
    }
}
