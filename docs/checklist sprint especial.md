# Checklist Sprint Especial — SI2 Grupo 2 (Backend)

> Estado real de implementación verificado en el código fuente.  
> Última verificación: Sprint Especial completado (excepto HU-SE-13 backend y HU-SE-04).  
> SQL de migración: [`scripts/db/sprint-especial-saas-migration.sql`](../scripts/db/sprint-especial-saas-migration.sql)

---

## Historias de usuario — Sprint Especial

| ID | Historia | SP | Backend | Frontend |
|----|----------|----|---------|----------|
| HU-SE-01 | Hardening multi-tenant | 2 | ✅ Completado | — |
| HU-SE-02 | Gestionar planes de suscripción | 5 | ✅ Completado | ✅ Completado |
| HU-SE-03 | Simular suscripción + validar límites | 5 | ✅ Completado | ✅ Completado |
| HU-SE-04 | Extender configuración paramétrica | 2 | ⬜ Pendiente | ⬜ Pendiente |
| HU-SE-05 | CRUD roles dinámicos por institución | 3 | ✅ Completado | ✅ Completado |
| HU-SE-06 | UI asignación de permisos a roles | 2 | ✅ Completado | ✅ Completado |
| HU-SE-07 | Privilegios campo/botón por rol | 8 | ✅ Completado | ✅ Completado |
| HU-SE-08 | Consulta de bitácora con filtros | 3 | ✅ Completado | ✅ Completado |
| HU-SE-09 | Intentos de login fallidos + consulta | 3 | ✅ Completado | ✅ Completado |
| HU-SE-10 | Registro de backups y restauraciones | 5 | ✅ Completado | ✅ Completado |
| HU-SE-11 | Reportes con filtros previos | 5 | ✅ Completado | ✅ Completado |
| HU-SE-12 | Reportes analíticos y gerenciales | 5 | ✅ Completado | ✅ Completado |
| HU-SE-13 | Reporte dinámico configurable | 8 | ❌ No implementado | ✅ Completado |

**Total estimado: 56 SP**

---

## Pre-requisito: Aplicar migración SQL

- [ ] Ejecutar `sprint-especial-saas-migration.sql` en RDS (producción)
- [ ] Verificar tablas creadas: `plan_suscripcion`, `modulo_sistema`, `plan_modulo`, `suscripcion_institucion`, `privilegio_ui`, `intento_login`, `registro_respaldo`, `registro_restauracion`, `bitacora_reporte`
- [ ] Verificar seed: 3 planes + 9 módulos + asociaciones plan-módulo + suscripción demo

> **Nota:** Las tablas `reporte_configurable` y `reporte_campo` no tienen entidades JPA aún (HU-SE-13 backend pendiente).

---

## HU-SE-01 — Hardening multi-tenant (2 SP) ✅

**Objetivo:** Asegurar que ninguna operación backend opere sin `id_institucion` cuando se requiere, y agregar claims de plan y módulos al JWT.

### Backend
- [x] En `TenantContext.java`: método `getOrThrow()` implementado (lanza `401` si el contexto es null)
- [x] `TenantContext.getOrThrow()` usado en todos los services de negocio (reemplaza llamadas a `get()`)
- [x] En `AuthService.buildToken()`: claims `plan_codigo` y `modulos_activos` (List<String>) agregados al JWT
- [x] En `JwtAuthFilter`: extrae y expone `plan_codigo` + `modulos_activos` desde claims
- [ ] Test de integración: verificar que request sin `id_institucion` en JWT retorna 401

---

## HU-SE-02 — Gestionar planes de suscripción (5 SP) ✅

**Objetivo:** Permitir al SUPER_ADMIN crear, editar y visualizar los planes del sistema.

### Base de datos
- [x] Tabla `plan_suscripcion` (en migración Sprint Especial)
- [x] Tabla `modulo_sistema` (en migración Sprint Especial)
- [x] Tabla `plan_modulo` (en migración Sprint Especial)
- [x] Seed de 3 planes + 9 módulos

### Backend
- [x] Entidad `PlanSuscripcion.java` + `PlanSuscripcionRepository`
- [x] Entidad `ModuloSistema.java` + `ModuloSistemaRepository`
- [x] Relación `@ManyToMany` en `PlanSuscripcion` → `Set<ModuloSistema> modulos` (no se usa entidad `PlanModulo.java` separada)
- [x] `PlanSuscripcionService` — CRUD completo, gestión de módulos del plan
- [x] `PlanSuscripcionController` — base path **`/api/saas/planes`** (difiere del spec `/api/planes`):
  - `GET  /api/saas/planes` — listar todos
  - `GET  /api/saas/planes/{id}` — detalle
  - `POST /api/saas/planes` — crear plan (SUPER_ADMIN)
  - `PUT  /api/saas/planes/{id}` — actualizar plan + módulos (SUPER_ADMIN)
  - `DELETE /api/saas/planes/{id}` — eliminar plan (SUPER_ADMIN)
- [x] `@PreAuthorize` en mutaciones
- [x] Registro en `bitacora_auditoria`

### Frontend
- [x] Feature `features/admin/saas/` visible para SUPER_ADMIN (no en `sia/planes/`)
- [x] Listado de planes con módulos incluidos
- [x] Formulario crear/editar plan
- [x] Conectado a `saas.service.ts`

---

## HU-SE-03 — Simular suscripción + validar límites (5 SP) ✅

**Objetivo:** Permitir que una institución seleccione un plan, y que el sistema valide el límite de usuarios al registrar nuevos.

### Base de datos
- [x] Tabla `suscripcion_institucion` (en migración Sprint Especial)

### Backend
- [x] Entidad `SuscripcionInstitucion.java` + `SuscripcionInstitucionRepository`
- [x] `SuscripcionInstitucionService`:
  - Suscribir institución a plan (valida que no haya otra ACTIVA)
  - Obtener plan activo de la institución actual
  - Obtener módulos activos de la institución
  - Validar `max_usuarios` antes de crear usuario
- [x] `SuscripcionInstitucionController` — base path **`/api/saas/suscripciones`**:
  - `GET  /api/saas/suscripciones/activa` — plan actual de la institución del JWT
  - `POST /api/saas/suscripciones` — suscribir institución a un plan
  - `DELETE /api/saas/suscripciones/activa` — cancelar suscripción activa (difiere del spec `PUT /{id}/cancelar`)
- [x] En `UsuarioService.crear()`: llamada a `SuscripcionService.validarLimiteUsuarios()` → 409 si excede
- [x] Registro en `bitacora_auditoria`
- [x] `SolicitudOnboardingController` — onboarding público + gestión SUPER_ADMIN:
  - `POST /api/public/solicitudes` — nueva solicitud (pública)
  - `GET  /api/saas/solicitudes` — listar (SUPER_ADMIN)
  - `GET  /api/saas/solicitudes/{id}` — detalle
  - `PUT  /api/saas/solicitudes/{id}/aprobar`
  - `PUT  /api/saas/solicitudes/{id}/rechazar`
  - `PUT  /api/saas/solicitudes/{id}/pago`
  - `POST /api/saas/solicitudes/{id}/activar`

### Frontend
- [x] Feature `features/sia/suscripcion/` — componente "Mi Plan"
- [x] Muestra plan activo, módulos, usuarios usados / máximo
- [x] Flujo de simulación: selector de planes → confirmación → activar
- [x] Conectado a `saas.service.ts`

---

## HU-SE-04 — Extender configuración paramétrica (2 SP) ⬜

**Objetivo:** Ampliar el uso de `ConfiguracionInstitucion` para que reglas de negocio variables se lean de configuración, no de código.

### Backend
- [ ] En `CalificacionService`: leer umbral de aprobación desde `ConfiguracionInstitucion` (clave: `UMBRAL_APROBACION`, default: `51`)
- [ ] En `AsistenciaService`: leer porcentaje mínimo de asistencia desde config (clave: `MIN_PORCENTAJE_ASISTENCIA`, default: `75`)
- [ ] Endpoint `GET /api/configuracion/catalogo` — lista claves estándar con descripción y valor por defecto
- [ ] Seed: insertar configuraciones por defecto si no existen

### Frontend
- [ ] Módulo `features/sia/configuracion/` ya existe: agregar sección "Reglas académicas"
- [ ] Campos: umbral de aprobación (número), porcentaje mínimo asistencia (número)
- [ ] Validar rango y tipo antes de guardar

---

## HU-SE-05 — CRUD roles dinámicos por institución (3 SP) ✅

**Objetivo:** Permitir al ADMIN_INSTITUCION crear roles personalizados para su institución (además de los roles globales fijos).

> **Nota:** Las tablas `rol`, `permiso`, `rol_permiso` ya existían desde Sprint 1.

### Backend
- [x] `RoleController` (`/api/roles`):
  - `GET  /api/roles` — listar roles globales + roles de la institución del JWT
  - `GET  /api/roles/asignables` — roles disponibles para asignación
  - `GET  /api/roles/permisos` — catálogo de todos los permisos del sistema
  - `POST /api/roles` — crear rol con `es_global=FALSE, id_institucion=<del JWT>` (ADMIN_INSTITUCION)
  - `PUT  /api/roles/{id}` — actualizar nombre/descripción + permisos asignados
  - `DELETE /api/roles/{id}` — desactivar rol
- [x] `PermissionCatalog.java` — catálogo de permisos del sistema
- [x] `RoleService.java` — lógica de negocio
- [x] Validación: no permitir modificar roles globales (`es_global=TRUE`) desde institución
- [x] Registro en `bitacora_auditoria`

### Frontend
- [x] Módulo `features/sia/roles/` — listado, creación y edición de roles
- [x] Indicación visual de roles globales (solo lectura) vs. propios de institución (editables)
- [x] Formulario de creación de rol con nombre y descripción

---

## HU-SE-06 — UI asignación de permisos a roles (2 SP) ✅

**Objetivo:** Permitir al ADMIN_INSTITUCION asignar y revocar permisos a los roles de su institución.

> **Nota:** Las tablas `permiso` y `rol_permiso` ya existían. Endpoints expuestos en `RoleController`.

### Backend
- [x] `GET  /api/roles/permisos` — listar todos los permisos del sistema (catálogo completo)
- [x] Asignación de permisos gestionada vía `PUT /api/roles/{id}` (incluye lista de permisos en el body)
- [x] Registro en `bitacora_auditoria`
- [x] Solo permite asignar permisos a roles de la propia institución

### Frontend
- [x] En módulo `roles/`: sección "Permisos del rol" con matriz de checkboxes
- [x] Guardar con confirmación
- [x] Feedback visual de permisos guardados

---

## HU-SE-07 — Privilegios campo/botón por rol (8 SP) ✅

**Objetivo:** Controlar visibilidad y editabilidad de campos y botones de formularios según el rol del usuario, sin duplicar formularios.

### Base de datos
- [x] Tabla `privilegio_ui` (en migración Sprint Especial)

### Backend
- [x] Entidad `PrivilegioUi.java` + `PrivilegioUiRepository`
- [x] `PrivilegioUiService`:
  - `obtenerPorRol(idInstitucion, idRol)` → mapa de privilegios por rol
  - `guardarPrivilegios(idInstitucion, idRol, List<PrivilegioUiRequest>)`
  - `obtenerPorUsuarioActual()` → construye mapa del usuario autenticado
- [x] `PrivilegioUiController` (`/api/privilegios-ui`):
  - `GET  /api/privilegios-ui/mi-mapa` — mapa de privilegios del usuario autenticado
  - `GET  /api/privilegios-ui/rol/{idRol}` — configuración por rol (ADMIN_INSTITUCION)
  - `PUT  /api/privilegios-ui/rol/{idRol}` — guardar configuración completa (ADMIN_INSTITUCION)
- [x] Registro en `bitacora_auditoria`

### Frontend
- [x] `AuthzService` en `core/services/authz.service.ts`:
  - `canView(modulo, entidad, campo): boolean`
  - `canEdit(modulo, entidad, campo): boolean`
  - Carga mapa de privilegios al hacer login desde `GET /api/privilegios-ui/mi-mapa`
- [x] Directiva `*appCanView` (oculta elemento) — `shared/components/can-view.directive.ts`
- [x] Directiva `[appCanEdit]` (deshabilita input) — `shared/components/can-edit.directive.ts`
- [x] Panel de administración de privilegios UI (en módulo `roles/`):
  - Selector de rol
  - Tabla por entidad con tres estados por campo: Editable / Solo lectura / Oculto

---

## HU-SE-08 — Consulta de bitácora con filtros (3 SP) ✅

**Objetivo:** Exponer la bitácora de auditoría (ya registrada) como endpoint consultable con filtros.

> **Nota:** `BitacoraAuditoria` y `AuditoriaService` ya existían y se usan en ~40 services.

### Backend
- [x] `AuditoriaController` (`/api/auditoria`):
  - `GET /api/auditoria` — listar con filtros paginados (`idUsuario`, `nombreModulo`, `tipoOperacion`, `fechaDesde`, `fechaHasta`, `exito`)
  - Seguridad: `SUPER_ADMIN` ve toda la plataforma; `ADMIN_INSTITUCION`/`DIRECTOR` solo su institución
- [x] `AuditoriaQueryService` — lógica de filtrado y paginación
- [x] `BitacoraAuditoriaFiltro` DTO — parámetros de filtro
- [x] `BitacoraAuditoriaResponse` DTO — subconjunto de campos expuestos
- [x] Paginación con `Pageable` (Spring Data)

### Frontend
- [x] Módulo `features/sia/auditoria/` — listado con filtros implementado
- [x] Filtros: módulo, operación, fecha desde/hasta, usuario, éxito/fallo
- [x] Tabla: fecha, usuario, módulo, operación, éxito, mensaje
- [x] Paginación del lado del servidor

---

## HU-SE-09 — Intentos de login fallidos + consulta (3 SP) ✅

**Objetivo:** Registrar cada intento de autenticación (exitoso y fallido) en `intento_login` y proveer endpoint de consulta.

### Base de datos
- [x] Tabla `intento_login` (en migración Sprint Especial)

### Backend
- [x] Entidad `IntentoLogin.java` + `IntentoLoginRepository`
- [x] `IntentoLoginService` — registra intento con correo, éxito, IP, agente, motivoFallo
- [x] En `AuthService.login()`: integración con `IntentoLoginService` para login exitoso y fallido
- [x] `IntentoLoginController` (`/api/auth/intentos-login`):
  - `GET /api/auth/intentos-login` — filtros por correo, solo_fallidos, fechaDesde, fechaHasta
- [x] `IntentoLoginResponse` DTO
- [x] Detección de múltiples fallos → alerta en `bitacora_auditoria`

### Frontend
- [x] Sección en módulo `auditoria/` o `seguridad/`: "Intentos de acceso"
- [x] Filtro de solo fallidos
- [x] Tabla: fecha, correo, IP, motivo, éxito/fallo
- [x] Badge de advertencia para correos con múltiples fallos

---

## HU-SE-10 — Registro de backups y restauraciones (5 SP)

**Objetivo:** Implementar registro de backups lógicos por tenant y flujo de solicitud de restauración, con aprobación de SUPER_ADMIN.

### Base de datos
- [x] Tabla `registro_respaldo` (en migración Sprint Especial)
- [x] Tabla `registro_restauracion` (en migración Sprint Especial)

### Backend
- [ ] Entidades `RegistroRespaldo.java`, `RegistroRestauracion.java` + Repositories
- [ ] `RespaldoService`:
  - `iniciarRespaldo(idInstitucion, tipo)` → crea registro con estado PENDIENTE → simula proceso → actualiza a COMPLETADO
  - Para `POR_TENANT`: genera nombre de ruta S3 lógica (no descarga real en v. académica)
  - `solicitarRestauracion(idRespaldo, motivo)` → crea `registro_restauracion` PENDIENTE
  - `aprobarRestauracion(idRestauracion)` → SUPER_ADMIN aprueba (APROBADO → simula → COMPLETADO)
- [x] `RespaldoController` (`/api`):
  - `GET  /api/respaldos` — historial con filtros ✅
  - `POST /api/respaldos` — iniciar backup ✅
  - `GET  /api/restauraciones` — historial de solicitudes ✅
  - `POST /api/restauraciones` — solicitar restauración ✅
  - `PUT  /api/restauraciones/{id}/aprobar` — aprobar (solo SUPER_ADMIN) ✅
- [x] Registro en `bitacora_auditoria`

### Frontend
- [x] Feature `features/sia/backups/` implementada:
  - Lista de backups con estado (badge color)
  - Botón "Iniciar backup"
  - Lista de solicitudes de restauración
  - Formulario solicitud de restauración
  - Para SUPER_ADMIN: botón aprobar restauración
- [x] `respaldo.service.ts` en `core/services/`

---

## HU-SE-11 — Reportes con filtros previos (5 SP) ✅

**Objetivo:** Todo reporte debe tener una pantalla previa de filtros. El usuario define parámetros antes de generar.

### Base de datos
- [x] Tabla `bitacora_reporte` (en migración Sprint Especial)

### Backend
- [x] `ReporteController` (`/api/reportes`):
  - `GET /api/reportes/asistencia` — con filtros (idGestion, idParalelo, idEstudiante, fechaDesde, fechaHasta, estadoAsistencia) ✅
  - `GET /api/reportes/calificaciones` — con filtros (idGestion, idParalelo, idMateria, idEstudiante, tipoEvaluacion) ✅
  - `GET /api/reportes/inscripciones` — con filtros (idGestion, idCurso, idParalelo, estado) ✅
- [x] Registro en `bitacora_reporte` al invocar endpoints
- [x] Paginación opcional

### Frontend
- [x] Feature `features/sia/reportes/` implementada:
  - Selector de tipo de reporte
  - Panel de filtros por tipo
  - Tabla de resultados (al pulsar "Generar")
- [x] `reporte.service.ts` en `core/services/`

---

## HU-SE-12 — Reportes analíticos y gerenciales (5 SP) ✅

**Objetivo:** Reportes detallados (analíticos) y reportes resumen (gerenciales/estratégicos).

### Backend
- [x] Reporte analítico de asistencia: detalle por sesión, estudiante, estado
- [x] Reporte analítico de calificaciones: detalle por evaluación, nota por estudiante
- [x] `GET /api/reportes/gerencial` — reporte gerencial unificado (asistencia + calificaciones + inscripciones) ✅
- [x] Estructura unificada `ReporteResponse<T>` con metadatos y `List<T> datos`

### Frontend
- [x] En módulo `reportes/`: vistas analítica y gerencial
- [x] Reporte gerencial: cards de KPI + tabla resumen
- [x] Reporte analítico: tabla detallada

---

## HU-SE-13 — Reporte dinámico configurable (8 SP) ❌ Backend pendiente

**Objetivo:** Permitir al usuario seleccionar columnas y filtros antes de generar. El Admin puede definir plantillas reutilizables.

> **Estado:** El frontend tiene la UI implementada en `features/sia/reportes/`. El backend NO tiene las entidades ni endpoints de reportes configurables.

### Base de datos
- [x] Tabla `reporte_configurable` (en migración Sprint Especial, creada en SQL pero sin entidad JPA)
- [x] Tabla `reporte_campo` (en migración Sprint Especial, creada en SQL pero sin entidad JPA)

### Backend ❌ PENDIENTE
- [ ] Entidad `ReporteConfigurable.java` + `ReporteConfigurableRepository` — **NO implementado**
- [ ] Entidad `ReporteCampo.java` + `ReporteCampoRepository` — **NO implementado**
- [ ] Directorio `reporte/domain/` no existe — **NO implementado**
- [ ] `ReporteService` para plantillas dinámicas — **NO implementado**
- [ ] Endpoints en `ReporteController`:
  - `GET  /api/reportes/plantillas` — **NO implementado**
  - `POST /api/reportes/plantillas` — **NO implementado**
  - `POST /api/reportes/generar/{idReporte}` — **NO implementado**
  - `GET  /api/reportes/campos/{entidad}` — **NO implementado**

### Frontend ✅ Completado
- [x] Sub-vista "Reportes dinámicos" en módulo `reportes/` implementada
- [x] Selector de plantilla, selección de columnas, filtros dinámicos

---

## Limpieza técnica

### Backend
- [ ] Implementar HU-SE-13 backend: entidades `ReporteConfigurable`, `ReporteCampo` + endpoints de plantillas dinámicas
- [ ] Implementar HU-SE-04: leer `UMBRAL_APROBACION` desde `ConfiguracionInstitucion` en `CalificacionService`
- [ ] Implementar HU-SE-04: leer `MIN_PORCENTAJE_ASISTENCIA` desde `ConfiguracionInstitucion` en `AsistenciaService`
- [ ] Agregar endpoint `GET /api/configuracion/catalogo` (HU-SE-04)
- [ ] Verificar que todos los `@PreAuthorize` sean correctos

### Frontend
- [x] `AuthzService` + directivas `can-view` / `can-edit` implementadas
- [x] Features SaaS: suscripcion, seguridad, roles, auditoria, backups, reportes, alertas
- [ ] Implementar UI de reportes dinámicos configurables conectada a backend cuando se complete HU-SE-13 backend
