# Checklist Backend — SI2 Grupo 2 · Sprint 2

> Stack: Spring Boot 3.5.0 · Java 23 · PostgreSQL (RDS) · Schema `sia` · JWT · Maven  
> Multitenancy: Shared DB, aislamiento por `id_institucion`  
> Migraciones: SQL manual (`scripts/db/`) — sin Flyway

---

## Historias de usuario Sprint 2

| ID | Historia | SP | Estado |
|----|----------|----|--------|
| HU-S2-14 | Gestionar aulas | 3 | ⬜ Pendiente |
| HU-S2-15 | Gestionar horarios | 8 | ⬜ Pendiente |
| HU-S2-16 | Registrar asistencia | 5 | ⬜ Pendiente |
| HU-S2-17 | Consultar asistencia | 3 | ⬜ Pendiente |
| HU-S2-18 | Gestionar calificaciones | 8 | ⬜ Pendiente |
| HU-S2-19 | Consultar historial académico | 5 | ⬜ Pendiente |

**Total estimado:** 32 SP

---

## Bloque 1 — Base de datos Sprint 2

> Agregar al final de `scripts/db/db-script.sql`. Mantener sección delimitada como `SPRINT 2`.

### Aulas (HU-S2-14)

- [ ] Tabla `aula`:
  - `id` UUID PK, `id_institucion` FK, `codigo` VARCHAR(30), `nombre` VARCHAR(120)`tipo_aula` VARCHAR(30) CHECK (`SALON`, `LABORATORIO`, `TALLER`, `AUDITORIO`, `OTRO`)
  - `capacidad` INTEGER, `ubicacion` VARCHAR(255), `estado` VARCHAR(15) DEFAULT `ACTIVO`
  - `creado_en`, `actualizado_en` TIMESTAMPTZ
  - UNIQUE (id_institucion, codigo), UNIQUE (id, id_institucion)
- [ ] Índice `idx_aula_institucion ON aula (id_institucion)`
- [ ] Trigger `actualizado_en` en `aula`

### Horarios (HU-S2-15)

- [ ] Tabla `horario`:
  - `id` UUID PK, `id_institucion` FK
  - `id_gestion_academica` FK compuesto con `id_institucion`
  - `id_paralelo` FK compuesto con `id_institucion`
  - `id_materia` FK compuesto con `id_institucion`
  - `id_docente` FK compuesto con `id_institucion`
  - `id_aula` FK compuesto con `id_institucion` (nullable)
  - `dia_semana` VARCHAR(10) CHECK (`LUNES`..`SABADO`)
  - `hora_inicio` TIME NOT NULL, `hora_fin` TIME NOT NULL
  - `tipo_horario` VARCHAR(30) DEFAULT `REGULAR` CHECK (`REGULAR`, `RECUPERACION`, `ESPECIAL`)
  - `estado` VARCHAR(15) DEFAULT `ACTIVO`
  - `creado_en`, `actualizado_en` TIMESTAMPTZ
  - UNIQUE (id_institucion, id_paralelo, id_materia, dia_semana, hora_inicio)
  - CHECK (hora_fin > hora_inicio)
  - UNIQUE (id, id_institucion)
- [ ] Índices: por `id_paralelo`, `id_docente`, `id_aula`, `dia_semana`
- [ ] Trigger `actualizado_en` en `horario`

### Asistencia (HU-S2-16, HU-S2-17)

- [ ] Tabla `sesion_asistencia`:
  - `id` UUID PK, `id_institucion` FK
  - `id_paralelo` FK, `id_materia` FK, `id_docente` FK, `id_gestion_academica` FK (todos compuestos)
  - `id_horario` UUID nullable (referencia al horario que originó la sesión)
  - `fecha` DATE NOT NULL
  - `hora_inicio` TIME, `hora_fin` TIME
  - `estado` VARCHAR(15) CHECK (`ABIERTA`, `CERRADA`) DEFAULT `ABIERTA`
  - `observacion` VARCHAR(255)
  - `creado_en`, `actualizado_en` TIMESTAMPTZ
  - UNIQUE (id_institucion, id_paralelo, id_materia, fecha) WHERE estado = 'ABIERTA'
  - UNIQUE (id, id_institucion)
- [ ] Tabla `registro_asistencia`:
  - `id` UUID PK, `id_institucion` FK
  - `id_sesion_asistencia` FK, `id_estudiante` FK (compuesto)
  - `estado_asistencia` VARCHAR(15) CHECK (`PRESENTE`, `AUSENTE`, `TARDE`, `JUSTIFICADO`) NOT NULL
  - `observacion` VARCHAR(255)
  - `creado_en`, `actualizado_en` TIMESTAMPTZ
  - UNIQUE (id_institucion, id_sesion_asistencia, id_estudiante)
  - UNIQUE (id, id_institucion)
- [ ] Índices por `id_sesion_asistencia`, `id_estudiante`, `fecha`
- [ ] Triggers `actualizado_en` en ambas tablas

### Calificaciones (HU-S2-18)

- [ ] Tabla `tipo_evaluacion`:
  - `id` UUID PK, `id_institucion` FK, `nombre` VARCHAR(100) NOT NULL
  - `descripcion` VARCHAR(255), `porcentaje` NUMERIC(5,2) CHECK (0 < porcentaje ≤ 100)
  - `estado` VARCHAR(15) DEFAULT `ACTIVO`
  - `creado_en`, `actualizado_en` TIMESTAMPTZ
  - UNIQUE (id_institucion, nombre), UNIQUE (id, id_institucion)
- [ ] Tabla `evaluacion`:
  - `id` UUID PK, `id_institucion` FK
  - `id_paralelo` FK, `id_materia` FK, `id_docente` FK, `id_tipo_evaluacion` FK, `id_gestion_academica` FK (todos compuestos)
  - `nombre` VARCHAR(120) NOT NULL, `descripcion` VARCHAR(255)
  - `fecha` DATE, `nota_maxima` NUMERIC(5,2) DEFAULT 100 CHECK (nota_maxima > 0)
  - `estado` VARCHAR(15) CHECK (`ACTIVA`, `CERRADA`, `ANULADA`) DEFAULT `ACTIVA`
  - `creado_en`, `actualizado_en` TIMESTAMPTZ
  - UNIQUE (id, id_institucion)
- [ ] Tabla `registro_calificacion`:
  - `id` UUID PK, `id_institucion` FK
  - `id_evaluacion` FK, `id_estudiante` FK (compuestos con `id_institucion`)
  - `nota` NUMERIC(5,2) CHECK (nota >= 0), `observacion` VARCHAR(255)
  - `estado` VARCHAR(15) CHECK (`PENDIENTE`, `REGISTRADA`, `ANULADA`) DEFAULT `PENDIENTE`
  - `creado_en`, `actualizado_en` TIMESTAMPTZ
  - UNIQUE (id_institucion, id_evaluacion, id_estudiante)
  - UNIQUE (id, id_institucion)
- [ ] Índices por `id_evaluacion`, `id_estudiante`
- [ ] Triggers `actualizado_en` en las 3 tablas

---

## Bloque 2 — Gestión de aulas (HU-S2-14)
> Nuevo paquete: `src/main/java/com/uagrm/si2g2/aula/`

- [ ] `Aula.java` — entidad JPA, campos según definición de DB
- [ ] `AulaRepository` — extiende JpaRepository, método `findByIdAndIdInstitucion`
- [ ] `AulaRequest` DTO — validaciones Bean Validation
- [ ] `AulaResponse` DTO
- [ ] `AulaService`:
  - [ ] `crear(request)` — validar código único por institución
  - [ ] `listar(idInstitucion)` — filtrar por institución
  - [ ] `buscarPorId(id, idInstitucion)` — validar tenencia
  - [ ] `actualizar(id, request, idInstitucion)`
  - [ ] `eliminar(id, idInstitucion)` — baja lógica (estado INACTIVO)
- [ ] `AulaController` — `POST /api/aulas`, `GET /api/aulas`, `GET /api/aulas/{id}`, `PUT /api/aulas/{id}`, `DELETE /api/aulas/{id}`
- [ ] `@PreAuthorize` por endpoint:
  - POST/PUT/DELETE → `ADMIN_INSTITUCION`, `SUPER_ADMIN`, `DIRECTOR`
  - GET → agregar `SECRETARIO`, `DOCENTE`

---

## Bloque 3 — Gestión de horarios (HU-S2-15)
> Nuevo paquete: `src/main/java/com/uagrm/si2g2/horario/`

- [ ] `Horario.java` — entidad JPA con relaciones a paralelo, materia, docente, aula, gestion
- [ ] `HorarioRepository` — métodos de consulta para detección de conflictos
- [ ] `HorarioRequest` DTO, `HorarioResponse` DTO
- [ ] `HorarioConflictoException` — excepción específica para conflictos (409)
- [ ] `HorarioService`:
  - [ ] `crear(request)` — validar los 3 tipos de conflicto antes de persistir
  - [ ] `validarConflictoDocente(idDocente, dia, inicio, fin, exceptoId)` — mismo docente mismo turno
  - [ ] `validarConflictoAula(idAula, dia, inicio, fin, exceptoId)` — misma aula mismo turno
  - [ ] `validarConflictoParalelo(idParalelo, dia, inicio, fin, exceptoId)` — mismo paralelo mismo turno
  - [ ] `listar(filtros)` — filtros: idParalelo, idDocente, idGestion, diaSemana, idInstitucion
  - [ ] `buscarPorId(id, idInstitucion)`
  - [ ] `actualizar(id, request)` — re-validar conflictos excluyendo el propio horario
  - [ ] `eliminar(id, idInstitucion)` — baja lógica
- [ ] `HorarioController` — `POST /api/horarios`, `GET /api/horarios`, `GET /api/horarios/{id}`, `PUT /api/horarios/{id}`, `DELETE /api/horarios/{id}`
- [ ] Respuesta 409 con mensaje descriptivo del tipo de conflicto (docente/aula/paralelo)
- [ ] `@PreAuthorize`:
  - POST/PUT/DELETE → `ADMIN_INSTITUCION`, `SUPER_ADMIN`, `DIRECTOR`
  - GET → agregar `SECRETARIO`, `DOCENTE`

---

## Bloque 4 — Registro de asistencia (HU-S2-16)
> Nuevo paquete: `src/main/java/com/uagrm/si2g2/asistencia/`

- [ ] `SesionAsistencia.java` — entidad JPA
- [ ] `RegistroAsistencia.java` — entidad JPA
- [ ] Repositories correspondientes
- [ ] DTOs: `SesionAsistenciaRequest`, `SesionAsistenciaResponse`, `RegistroAsistenciaRequest`, `RegistroAsistenciaResponse`
- [ ] `SesionAsistenciaService`:
  - [ ] `abrirSesion(request)` — validar que no exista sesión abierta para mismo paralelo+materia+fecha
  - [ ] `cerrarSesion(id, idInstitucion)` — cambiar estado a CERRADA
  - [ ] `buscarPorId(id, idInstitucion)`
  - [ ] `listar(filtros)` — idParalelo, idMateria, idGestion, idDocente, fecha, idInstitucion
- [ ] `RegistroAsistenciaService`:
  - [ ] `registrarBatch(idSesion, listaRegistros)` — upsert masivo de la lista completa de estudiantes
  - [ ] `actualizarRegistro(idSesion, idEstudiante, nuevoEstado)` — actualización individual
  - [ ] `listarPorSesion(idSesion, idInstitucion)`
- [ ] `SesionAsistenciaController`:
  - [ ] `POST /api/sesiones-asistencia` — abrir sesión
  - [ ] `GET /api/sesiones-asistencia` — listar con filtros
  - [ ] `GET /api/sesiones-asistencia/{id}` — detalle
  - [ ] `POST /api/sesiones-asistencia/{id}/cerrar` — cerrar sesión
  - [ ] `POST /api/sesiones-asistencia/{id}/registros` — carga masiva de asistencia
  - [ ] `GET /api/sesiones-asistencia/{id}/registros` — listar registros de la sesión
  - [ ] `PUT /api/sesiones-asistencia/{idSesion}/registros/{idEstudiante}` — actualizar registro individual
- [ ] `@PreAuthorize`:
  - Abrir/cerrar sesión → `DOCENTE`, `ADMIN_INSTITUCION`, `SUPER_ADMIN`
  - Registrar asistencia → `DOCENTE`, `ADMIN_INSTITUCION`, `SUPER_ADMIN`
  - Consultar → agregar `DIRECTOR`, `SECRETARIO`
- [ ] Bitácora: registrar apertura y cierre de sesión

---

## Bloque 5 — Consulta de asistencia (HU-S2-17)

> Dentro del mismo paquete `asistencia/`

- [ ] Endpoint `GET /api/sesiones-asistencia` con filtros extendidos (fecha rango, estado sesión)
- [ ] Endpoint `GET /api/estudiantes/{id}/asistencia` — historial del estudiante
  - Parámetros: `?idGestion=`, `?idParalelo=`, `?idMateria=`
  - Respuesta incluye: lista de sesiones, estado por sesión, total y porcentaje de asistencia
- [ ] Método `calcularEstadisticasAsistencia(idEstudiante, filtros)` en service
- [ ] `@PreAuthorize`: `ADMIN_INSTITUCION`, `SUPER_ADMIN`, `DIRECTOR`, `SECRETARIO`, `DOCENTE`; el propio `ESTUDIANTE` puede ver solo sus datos

---

## Bloque 6 — Gestión de calificaciones (HU-S2-18)
> Nuevo paquete: `src/main/java/com/uagrm/si2g2/calificacion/`

- [ ] `TipoEvaluacion.java` + `TipoEvaluacionRepository`
- [ ] `Evaluacion.java` + `EvaluacionRepository`
- [ ] `RegistroCalificacion.java` + `RegistroCalificacionRepository`
- [ ] DTOs para los 3 recursos
- [ ] `TipoEvaluacionService` — CRUD, validar nombre único por institución
- [ ] `TipoEvaluacionController` — `POST /api/tipos-evaluacion`, `GET`, `GET/{id}`, `PUT/{id}`, `DELETE/{id}`
- [ ] `EvaluacionService`:
  - [ ] `crear(request)` — validar pertenencia del docente a la materia+paralelo
  - [ ] `listar(filtros)` — idParalelo, idMateria, idGestion, idDocente
  - [ ] `cerrar(id)` — cambiar estado a CERRADA
  - [ ] CRUD completo
- [ ] `EvaluacionController` — `POST /api/evaluaciones`, `GET`, `GET/{id}`, `PUT/{id}`, `DELETE/{id}`, `POST /{id}/cerrar`
- [ ] `RegistroCalificacionService`:
  - [ ] `registrarBatch(idEvaluacion, listaNotas)` — carga masiva
  - [ ] `actualizarNota(id, nuevaNota)` — solo si estado != ANULADA; validar nota ≤ nota_maxima
  - [ ] `anular(id)` — cambiar estado a ANULADA + bitácora con datos_antes
  - [ ] `listarPorEvaluacion(idEvaluacion, idInstitucion)`
- [ ] `RegistroCalificacionController`:
  - [ ] `POST /api/evaluaciones/{id}/calificaciones` — carga masiva
  - [ ] `GET /api/evaluaciones/{id}/calificaciones` — listar notas de la evaluación
  - [ ] `PUT /api/evaluaciones/{idEval}/calificaciones/{idEstudiante}` — actualizar nota
- [ ] `@PreAuthorize`:
  - Tipos: CRUD → `ADMIN_INSTITUCION`, `SUPER_ADMIN`, `DIRECTOR`; lectura → `DOCENTE`
  - Evaluaciones: crear/editar → `DOCENTE`, `ADMIN_INSTITUCION`, `SUPER_ADMIN`; eliminar → `ADMIN_INSTITUCION`, `SUPER_ADMIN`
  - Calificaciones: registrar/editar → `DOCENTE`; anular → `ADMIN_INSTITUCION`, `SUPER_ADMIN`
- [ ] Bitácora: cambios de nota con `datos_antes` y `datos_despues`

---

## Bloque 7 — Historial académico (HU-S2-19)

> Puede implementarse como método adicional en `EstudianteController` / `EstudianteService` o en un paquete `historial/` si la complejidad lo justifica.

- [ ] `HistorialAcademicoResponse` DTO — contiene:
  - Datos del estudiante (id, nombre, código)
  - Lista de gestiones con: paralelo, curso, inscripción, lista de materias, por materia: lista de evaluaciones con notas y promedio, total sesiones, sesiones asistidas, % asistencia
- [ ] `HistorialService.obtenerHistorial(idEstudiante, idGestion, idInstitucion)` — consulta agregada
- [ ] Endpoint `GET /api/estudiantes/{id}/historial`
  - Parámetro opcional `?idGestion=uuid`
  - Sin `idGestion` devuelve todas las gestiones del estudiante
- [ ] `@PreAuthorize`:
  - `ADMIN_INSTITUCION`, `SUPER_ADMIN`, `DIRECTOR`, `SECRETARIO`, `DOCENTE`
  - El propio estudiante (`ESTUDIANTE`) puede ver solo su historial

---

## Bloque 8 — Multitenancy

- [ ] Todos los nuevos paquetes filtran por `id_institucion` en capa service
- [ ] Ningún endpoint devuelve datos de otra institución
- [ ] FKs compuestas `(id, id_institucion)` en las nuevas tablas para integridad cross-tenant
- [ ] `TenantContext` utilizado correctamente en todos los nuevos services

---

## Bloque 9 — Auditoría

- [ ] `AuditoriaService` invocado en:
  - [ ] Apertura de sesión de asistencia
  - [ ] Cierre de sesión de asistencia
  - [ ] Registro masivo de asistencia
  - [ ] Registro de calificaciones (con datos_antes)
  - [ ] Modificación de notas (con datos_antes + datos_despues)
  - [ ] Anulación de calificación

---

## Bloque 10 — Verificación y entregables

- [ ] App compila (`mvn package` BUILD SUCCESS)
- [ ] App inicia contra PostgreSQL RDS sin errores
- [ ] Tablas Sprint 2 creadas en RDS y `db-script.sql` actualizado
- [ ] `POST /api/aulas` — crear aula exitosamente
- [ ] `POST /api/horarios` con conflicto devuelve 409 con descripción
- [ ] `POST /api/sesiones-asistencia` — abrir sesión
- [ ] `POST /api/sesiones-asistencia/{id}/registros` — carga masiva de asistencia
- [ ] `POST /api/evaluaciones` — crear evaluación
- [ ] `POST /api/evaluaciones/{id}/calificaciones` — carga masiva de notas
- [ ] `GET /api/estudiantes/{id}/historial` — respuesta consolidada correcta
- [ ] Endpoints nuevos protegidos por rol verificados
- [ ] Bitácora registra operaciones de asistencia y calificaciones
- [ ] Todos los endpoints nuevos probados en Postman

---

## Notas técnicas

```powershell
# Arrancar la app (carga .env automáticamente via spring.config.import)
mvn spring-boot:run

# Agregar nuevas tablas manualmente en RDS
psql -h <RDS_HOST> -U <USUARIO> -d <DB_NAME> -f scripts/db/db-script.sql
```
