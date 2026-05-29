# Pruebas Postman — Sprint 2 (HU-S2-14 a HU-S2-19)

Sprint: **Operación académica diaria: horarios, asistencia y calificaciones**

## Archivos

| Archivo | Descripción |
| --- | --- |
| `Sprint-2-Operacion-Academica.postman_collection.json` | Colección con requests por historia de usuario |
| `Sprint-2-Demo.postman_environment.json` | Variables de entorno (baseUrl, tokens, IDs) |

Script automatizado alternativo: `scripts/test-sprint2-api.ps1`

## Pre-requisitos

1. Backend corriendo en `http://localhost:2026` (`docker compose up -d`).
2. Migraciones Sprint 2 aplicadas en PostgreSQL (schema `sia`):

```bash
# Desde la raíz del repo (requiere Docker)
docker run --rm -v "./si2-g2-backend-springboot/scripts/db:/scripts" \
  -e PGPASSWORD=<DB_PASSWORD> postgres:16 \
  psql -h host.docker.internal -p 5433 -U postgres -d academic \
  -f /scripts/sprint2-horarios-migration.sql

docker run --rm -v "./si2-g2-backend-springboot/scripts/db:/scripts" \
  -e PGPASSWORD=<DB_PASSWORD> postgres:16 \
  psql -h host.docker.internal -p 5433 -U postgres -d academic \
  -f /scripts/sprint2-asistencia-migration.sql

docker run --rm -v "./si2-g2-backend-springboot/scripts/db:/scripts" \
  -e PGPASSWORD=<DB_PASSWORD> postgres:16 \
  psql -h host.docker.internal -p 5433 -U postgres -d academic \
  -f /scripts/sprint2-evaluacion-materia-migration.sql
```

3. Datos demo: carpeta **00 - Setup** → Login SUPER_ADMIN → POST Seed synthetic.

Usuarios: ver [usuarios-seed.md](../usuarios-seed.md). Contraseña demo: `Demo12345!`.

## Importar en Postman

1. **Import** → seleccionar ambos JSON de esta carpeta.
2. Activar entorno **SI2-G2 Sprint 2 Demo**.
3. Actualizar `fechaHoy` con la fecha actual.
4. Ejecutar carpeta **00 - Setup** en orden.
5. Ejecutar cada carpeta HU en secuencia (o Collection Runner).

## Resultados de prueba automatizada (2026-05-29)

| HU | Nombre | Tests | Resultado |
| --- | --- | --- | --- |
| HU-S2-14 | Gestionar aulas | 6/6 | OK |
| HU-S2-15 | Gestionar horarios | 5/5 | OK (incl. validación conflicto 409) |
| HU-S2-16 | Registrar asistencia | 4/4 | OK |
| HU-S2-17 | Consultar asistencia | 4/4 | OK (estudiante/tutor → 403 en mis-asignaciones) |
| HU-S2-18 | Gestionar calificaciones | 6/6 | OK |
| HU-S2-19 | Consultar historial académico | 3/3 | OK |

## Endpoints por HU

### HU-S2-14 — Gestionar aulas (DIRECTOR / ADMIN)

| Método | Endpoint | Criterio |
| --- | --- | --- |
| POST | `/api/aulas` | Registrar aula (código único, capacidad, recursos) |
| GET | `/api/aulas` | Listado con filtros `estado`, `capacidadMin/Max`, `recurso`, `q` |
| GET | `/api/aulas/{id}` | Detalle |
| PUT | `/api/aulas/{id}` | Editar |
| DELETE | `/api/aulas/{id}` | Desactivar (bloqueado si tiene horarios activos) |

### HU-S2-15 — Gestionar horarios (DIRECTOR / ADMIN)

| Método | Endpoint | Criterio |
| --- | --- | --- |
| POST | `/api/horarios` | Crear con materia/docente/grupo/aula/día/hora |
| GET | `/api/horarios?idInstitucion=` | Listar activos |
| GET | `/api/horarios/asignacion/{id}` | Por docente-asignación |
| GET | `/api/horarios/aula/{id}` | Por aula |
| PUT/DELETE | `/api/horarios/{id}` | Editar / desactivar |
| POST (conflicto) | `/api/horarios` | Retorna **409** si docente/aula/grupo se solapan |

### HU-S2-16 — Registrar asistencia (DOCENTE)

| Método | Endpoint | Criterio |
| --- | --- | --- |
| GET | `/api/asistencias/mis-asignaciones` | Asignaciones del docente |
| GET | `/api/asistencias/plantilla?idAsignacionDocente=&fecha=` | Lista estudiantes del día |
| POST | `/api/asistencias` | Registro masivo (PRESENTE/AUSENTE/TARDANZA/JUSTIFICADO) |
| GET | `/api/asistencias/{id}` | Consultar registro |

### HU-S2-17 — Consultar asistencia

| Rol | Comportamiento verificado |
| --- | --- |
| DIRECTOR | Accede a plantilla y registro por ID |
| DOCENTE | Mismos endpoints de lectura en sus materias |
| ESTUDIANTE / TUTOR | **403** en `mis-asignaciones` (sin permiso directo; historial vía HU-S2-19) |

> Nota: no hay endpoint dedicado de resumen porcentual de asistencia; la consulta se hace vía plantilla/registro e historial académico.

### HU-S2-18 — Gestionar calificaciones (DOCENTE)

| Método | Endpoint | Criterio |
| --- | --- | --- |
| GET | `/api/calificaciones/mis-asignaciones` | Asignaciones disponibles |
| POST | `/api/calificaciones/evaluaciones` | Crear evaluación (`escala`: NUMERICA o LITERAL) |
| GET | `/api/calificaciones/evaluaciones?idMateria=&periodo=` | Listar evaluaciones |
| GET | `/api/calificaciones/plantilla?idEvaluacion=` | Plantilla por estudiante |
| POST | `/api/calificaciones` | Registrar notas (auditoría en cambios) |
| GET | `/api/calificaciones/resumen?idAsignacionDocente=&periodo=` | Nota consolidada del período |

### HU-S2-19 — Historial académico

| Método | Endpoint | Roles |
| --- | --- | --- |
| GET | `/api/estudiantes/{id}/historial?idGestion=` (opcional) | ESTUDIANTE, TUTOR, DOCENTE, DIRECTOR |

Respuesta incluye materias, calificaciones por evaluación y asistencia promedio por gestión.

## Ejecutar script PowerShell

```powershell
powershell -ExecutionPolicy Bypass -File .\si2-g2-backend-springboot\scripts\test-sprint2-api.ps1
```

Variables de entorno opcionales: `$env:API_BASE = "http://localhost:2026"`.
