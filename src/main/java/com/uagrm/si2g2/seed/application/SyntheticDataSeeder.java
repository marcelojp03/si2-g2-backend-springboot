package com.uagrm.si2g2.seed.application;

import com.uagrm.si2g2.academico.domain.GestionAcademica;
import com.uagrm.si2g2.academico.domain.GestionAcademicaRepository;
import com.uagrm.si2g2.asignacion.domain.AsignacionDocente;
import com.uagrm.si2g2.asignacion.domain.AsignacionDocenteRepository;
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

@Service
@RequiredArgsConstructor
public class SyntheticDataSeeder {

    private static final String PASSWORD = "Demo12345!";
    private static final String INSTITUCION_CODIGO = "SEED-001";
    private static final String GESTION_NOMBRE = "Gestion Academica Demo 2026";

    private final InstitucionRepository institucionRepository;
    private final GestionAcademicaRepository gestionAcademicaRepository;
    private final CursoRepository cursoRepository;
    private final ParaleloRepository paraleloRepository;
    private final MateriaRepository materiaRepository;
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

    @Transactional
    public SeedResult seed() {
        SeedStats stats = new SeedStats();
        List<SeedUser> usuarios = new ArrayList<>();

        Roles roles = loadRoles();
        Institucion institucion = mark(stats, "instituciones", findOrCreateInstitucion());
        UUID idInstitucion = institucion.getId();

        GestionAcademica gestion = mark(stats, "gestiones", findOrCreateGestion(idInstitucion));

        Usuario admin = mark(stats, "usuarios", findOrCreateUsuario(
                "admin.demo@si2.test", "Admin", "Institucion Demo", idInstitucion, roles.adminInstitucion()));
        usuarios.add(user("admin.demo@si2.test", "ADMIN_INSTITUCION", "Admin Institucion Demo", "Acceso administrativo de institucion"));

        Usuario director = mark(stats, "usuarios", findOrCreateUsuario(
                "director.demo@si2.test", "Daniel", "Quiroga", idInstitucion, roles.director()));
        usuarios.add(user("director.demo@si2.test", "DIRECTOR", "Daniel Quiroga", "Director academico"));

        Usuario secretario = mark(stats, "usuarios", findOrCreateUsuario(
                "secretaria.demo@si2.test", "Mariela", "Ribera", idInstitucion, roles.secretario()));
        usuarios.add(user("secretaria.demo@si2.test", "SECRETARIO", "Mariela Ribera", "Secretaria academica"));

        Usuario docenteMate = mark(stats, "usuarios", findOrCreateUsuario(
                "docente.mate.demo@si2.test", "Ana", "Rojas", idInstitucion, roles.docente()));
        Usuario docenteLeng = mark(stats, "usuarios", findOrCreateUsuario(
                "docente.lenguaje.demo@si2.test", "Carlos", "Mendez", idInstitucion, roles.docente()));
        usuarios.add(user("docente.mate.demo@si2.test", "DOCENTE", "Ana Rojas", "Docente de Matematica"));
        usuarios.add(user("docente.lenguaje.demo@si2.test", "DOCENTE", "Carlos Mendez", "Docente de Lenguaje"));

        Usuario estudianteLucia = mark(stats, "usuarios", findOrCreateUsuario(
                "estudiante.lucia.demo@si2.test", "Lucia", "Vargas", idInstitucion, roles.estudiante()));
        Usuario estudianteMateo = mark(stats, "usuarios", findOrCreateUsuario(
                "estudiante.mateo.demo@si2.test", "Mateo", "Flores", idInstitucion, roles.estudiante()));
        Usuario estudianteSofia = mark(stats, "usuarios", findOrCreateUsuario(
                "estudiante.sofia.demo@si2.test", "Sofia", "Rojas", idInstitucion, roles.estudiante()));
        usuarios.add(user("estudiante.lucia.demo@si2.test", "ESTUDIANTE", "Lucia Vargas", "Estudiante de 1ro Primaria A"));
        usuarios.add(user("estudiante.mateo.demo@si2.test", "ESTUDIANTE", "Mateo Flores", "Estudiante de 1ro Primaria A"));
        usuarios.add(user("estudiante.sofia.demo@si2.test", "ESTUDIANTE", "Sofia Rojas", "Estudiante de 2do Primaria A"));

        Usuario tutorMaria = mark(stats, "usuarios", findOrCreateUsuario(
                "tutor.maria.demo@si2.test", "Maria", "Lopez", idInstitucion, roles.tutor()));
        Usuario tutorJorge = mark(stats, "usuarios", findOrCreateUsuario(
                "tutor.jorge.demo@si2.test", "Jorge", "Flores", idInstitucion, roles.tutor()));
        usuarios.add(user("tutor.maria.demo@si2.test", "TUTOR", "Maria Lopez", "Tutora de Lucia y Sofia"));
        usuarios.add(user("tutor.jorge.demo@si2.test", "TUTOR", "Jorge Flores", "Tutor de Mateo"));

        Curso primeroPrimaria = mark(stats, "cursos", findOrCreateCurso(idInstitucion, "PRI-1", "1ro Primaria", "Primaria", 1));
        Curso segundoPrimaria = mark(stats, "cursos", findOrCreateCurso(idInstitucion, "PRI-2", "2do Primaria", "Primaria", 2));
        Curso primeroSecundaria = mark(stats, "cursos", findOrCreateCurso(idInstitucion, "SEC-1", "1ro Secundaria", "Secundaria", 7));

        Paralelo primeroA = mark(stats, "paralelos", findOrCreateParalelo(idInstitucion, primeroPrimaria.getId(), gestion.getId(), "A", 30));
        Paralelo segundoA = mark(stats, "paralelos", findOrCreateParalelo(idInstitucion, segundoPrimaria.getId(), gestion.getId(), "A", 30));
        Paralelo secundariaA = mark(stats, "paralelos", findOrCreateParalelo(idInstitucion, primeroSecundaria.getId(), gestion.getId(), "A", 35));

        Materia matematica = mark(stats, "materias", findOrCreateMateria(idInstitucion, "MAT-DEMO", "Matematica", "Ciencias Exactas", 6));
        Materia lenguaje = mark(stats, "materias", findOrCreateMateria(idInstitucion, "LEN-DEMO", "Lenguaje", "Comunicacion", 5));
        Materia ciencias = mark(stats, "materias", findOrCreateMateria(idInstitucion, "CN-DEMO", "Ciencias Naturales", "Ciencias", 4));

        mark(stats, "curso_materias", findOrCreateCursoMateria(idInstitucion, primeroPrimaria.getId(), matematica.getId(), gestion.getId(), 6));
        mark(stats, "curso_materias", findOrCreateCursoMateria(idInstitucion, primeroPrimaria.getId(), lenguaje.getId(), gestion.getId(), 5));
        mark(stats, "curso_materias", findOrCreateCursoMateria(idInstitucion, segundoPrimaria.getId(), matematica.getId(), gestion.getId(), 6));
        mark(stats, "curso_materias", findOrCreateCursoMateria(idInstitucion, segundoPrimaria.getId(), ciencias.getId(), gestion.getId(), 4));
        mark(stats, "curso_materias", findOrCreateCursoMateria(idInstitucion, primeroSecundaria.getId(), matematica.getId(), gestion.getId(), 6));
        mark(stats, "curso_materias", findOrCreateCursoMateria(idInstitucion, primeroSecundaria.getId(), lenguaje.getId(), gestion.getId(), 5));

        Docente docenteMatematica = mark(stats, "docentes", findOrCreateDocente(
                idInstitucion, docenteMate.getId(), "DOC-DEMO-001", "9001001", "Ana", "Rojas",
                "76000001", "docente.mate.demo@si2.test", "Matematica"));
        Docente docenteLenguaje = mark(stats, "docentes", findOrCreateDocente(
                idInstitucion, docenteLeng.getId(), "DOC-DEMO-002", "9001002", "Carlos", "Mendez",
                "76000002", "docente.lenguaje.demo@si2.test", "Lenguaje y Comunicacion"));

        Estudiante lucia = mark(stats, "estudiantes", findOrCreateEstudiante(
                idInstitucion, estudianteLucia.getId(), "EST-DEMO-001", "9101001", "Lucia", "Vargas",
                LocalDate.of(2018, 3, 12), "FEMENINO", "Barrio Equipetrol", "77000001", "estudiante.lucia.demo@si2.test"));
        Estudiante mateo = mark(stats, "estudiantes", findOrCreateEstudiante(
                idInstitucion, estudianteMateo.getId(), "EST-DEMO-002", "9101002", "Mateo", "Flores",
                LocalDate.of(2018, 7, 20), "MASCULINO", "Barrio Hamacas", "77000002", "estudiante.mateo.demo@si2.test"));
        Estudiante sofia = mark(stats, "estudiantes", findOrCreateEstudiante(
                idInstitucion, estudianteSofia.getId(), "EST-DEMO-003", "9101003", "Sofia", "Rojas",
                LocalDate.of(2017, 10, 5), "FEMENINO", "Barrio Las Palmas", "77000003", "estudiante.sofia.demo@si2.test"));

        Tutor maria = mark(stats, "tutores", findOrCreateTutor(
                idInstitucion, tutorMaria.getId(), "9201001", "Maria", "Lopez",
                "78000001", "tutor.maria.demo@si2.test", "Barrio Equipetrol"));
        Tutor jorge = mark(stats, "tutores", findOrCreateTutor(
                idInstitucion, tutorJorge.getId(), "9201002", "Jorge", "Flores",
                "78000002", "tutor.jorge.demo@si2.test", "Barrio Hamacas"));

        mark(stats, "estudiante_tutores", findOrCreateEstudianteTutor(idInstitucion, lucia.getId(), maria.getId(), "Madre", true));
        mark(stats, "estudiante_tutores", findOrCreateEstudianteTutor(idInstitucion, mateo.getId(), jorge.getId(), "Padre", true));
        mark(stats, "estudiante_tutores", findOrCreateEstudianteTutor(idInstitucion, sofia.getId(), maria.getId(), "Tia", true));

        mark(stats, "inscripciones", findOrCreateInscripcion(idInstitucion, lucia.getId(), gestion.getId(), primeroA.getId(), LocalDate.of(2026, 2, 3)));
        mark(stats, "inscripciones", findOrCreateInscripcion(idInstitucion, mateo.getId(), gestion.getId(), primeroA.getId(), LocalDate.of(2026, 2, 3)));
        mark(stats, "inscripciones", findOrCreateInscripcion(idInstitucion, sofia.getId(), gestion.getId(), segundoA.getId(), LocalDate.of(2026, 2, 4)));

        mark(stats, "asignaciones", findOrCreateAsignacion(idInstitucion, docenteMatematica.getId(), matematica.getId(), primeroA.getId(), gestion.getId()));
        mark(stats, "asignaciones", findOrCreateAsignacion(idInstitucion, docenteLenguaje.getId(), lenguaje.getId(), primeroA.getId(), gestion.getId()));
        mark(stats, "asignaciones", findOrCreateAsignacion(idInstitucion, docenteMatematica.getId(), matematica.getId(), segundoA.getId(), gestion.getId()));
        mark(stats, "asignaciones", findOrCreateAsignacion(idInstitucion, docenteLenguaje.getId(), lenguaje.getId(), secundariaA.getId(), gestion.getId()));

        return new SeedResult(INSTITUCION_CODIGO, GESTION_NOMBRE, stats.creados(), stats.existentes(), usuarios);
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

    private SeedEntity<Institucion> findOrCreateInstitucion() {
        return findOrCreate(
                () -> institucionRepository.findByCodigo(INSTITUCION_CODIGO).orElse(null),
                () -> institucionRepository.save(Institucion.builder()
                        .codigo(INSTITUCION_CODIGO)
                        .nombre("Colegio Demo Semilla")
                        .tipoInstitucion("PRIVADO")
                        .telefono("70010000")
                        .correo("contacto.seed@si2.test")
                        .direccion("Av. Demo 123, Santa Cruz")
                        .build())
        );
    }

    private SeedEntity<GestionAcademica> findOrCreateGestion(UUID idInstitucion) {
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

    private SeedEntity<Curso> findOrCreateCurso(UUID idInstitucion, String codigo, String nombre, String nivel, int ordenVisual) {
        return findOrCreate(
                () -> cursoRepository.findAllByIdInstitucion(idInstitucion).stream()
                        .filter(c -> codigo.equals(c.getCodigo()))
                        .findFirst()
                        .orElse(null),
                () -> {
                    Curso curso = Curso.builder()
                            .idInstitucion(idInstitucion)
                            .codigo(codigo)
                            .nombre(nombre)
                            .nivel(nivel)
                            .build();
                    Curso saved = cursoRepository.save(curso);
                    entityManager.flush();
                    jdbcTemplate.update("UPDATE sia.curso SET orden_visual = ? WHERE id = ?", ordenVisual, saved.getId());
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

    private SeedEntity<Materia> findOrCreateMateria(UUID idInstitucion, String codigo, String nombre, String area, int cargaHoraria) {
        return findOrCreate(
                () -> materiaRepository.findAllByIdInstitucion(idInstitucion).stream()
                        .filter(m -> codigo.equals(m.getCodigo()))
                        .findFirst()
                        .orElse(null),
                () -> materiaRepository.save(Materia.builder()
                        .idInstitucion(idInstitucion)
                        .codigo(codigo)
                        .nombre(nombre)
                        .area(area)
                        .cargaHoraria(cargaHoraria)
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

    private record Roles(
            Rol adminInstitucion,
            Rol director,
            Rol secretario,
            Rol docente,
            Rol estudiante,
            Rol tutor
    ) {
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
