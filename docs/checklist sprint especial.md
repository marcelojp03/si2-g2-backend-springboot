# Checklist Sprint Especial — SI2 Grupo 2 (Combinado)

> Vista unificada del Sprint Especial SaaS.  
> Documento de planificación: [`docs/sprint-especial.md`](../sprint-especial.md)  
> SQL de migración: [`si2-g2-backend-springboot/scripts/db/sprint-especial-saas-migration.sql`](../si2-g2-backend-springboot/scripts/db/sprint-especial-saas-migration.sql)

---

## Historias de usuario — Sprint Especial

| ID | Historia | SP | Backend | Frontend |
|----|----------|----|---------|----------|
| HU-SE-01 | Hardening multi-tenant | 2 | ⬜ Pendiente | — |
| HU-SE-02 | Gestionar planes de suscripción | 5 | ⬜ Pendiente | ⬜ Pendiente |
| HU-SE-03 | Simular suscripción + validar límites | 5 | ⬜ Pendiente | ⬜ Pendiente |
| HU-SE-04 | Extender configuración paramétrica | 2 | ⬜ Pendiente | ⬜ Pendiente |
| HU-SE-05 | CRUD roles dinámicos por institución | 3 | ⬜ Pendiente | ⬜ Pendiente |
| HU-SE-06 | UI asignación de permisos a roles | 2 | ⬜ Pendiente | ⬜ Pendiente |
| HU-SE-07 | Privilegios campo/botón por rol | 8 | ⬜ Pendiente | ⬜ Pendiente |
| HU-SE-08 | Consulta de bitácora con filtros | 3 | ⬜ Pendiente | ⬜ Pendiente |
| HU-SE-09 | Intentos de login fallidos + consulta | 3 | ⬜ Pendiente | ⬜ Pendiente |
| HU-SE-10 | Registro de backups y restauraciones | 5 | ⬜ Pendiente | ⬜ Pendiente |
| HU-SE-11 | Reportes con filtros previos | 5 | ⬜ Pendiente | ⬜ Pendiente |
| HU-SE-12 | Reportes analíticos y gerenciales | 5 | ⬜ Pendiente | ⬜ Pendiente |
| HU-SE-13 | Reporte dinámico configurable | 8 | ⬜ Pendiente | ⬜ Pendiente |

**Total estimado: 56 SP**

---

## Pre-requisito: Aplicar migración SQL

- [ ] Ejecutar `sprint-especial-saas-migration.sql` en RDS (producción)
- [ ] Verificar 11 tablas creadas: `plan_suscripcion`, `modulo_sistema`, `plan_modulo`, `suscripcion_institucion`, `privilegio_ui`, `intento_login`, `registro_respaldo`, `registro_restauracion`, `reporte_configurable`, `reporte_campo`, `bitacora_reporte`
- [ ] Verificar seed: 3 planes + 9 módulos + asociaciones plan-módulo + suscripción demo

---

## HU-SE-01 — Hardening multi-tenant (2 SP)

**Objetivo:** Asegurar que ninguna operación backend opere sin `id_institucion` cuando se requiere, y agregar claims de plan y módulos al JWT.

### Backend
- [ ] En `TenantContext.java`: agregar método `getOrThrow()` que lanza `401` si el contexto es null
- [ ] Reemplazar `TenantContext.get()` por `TenantContext.getOrThrow()` en todos los services de negocio
- [ ] En `AuthService.buildToken()`: agregar claims `plan_codigo` y `modulos_activos` (List<String>) al JWT
- [ ] En `JwtAuthFilter`: extraer y exponer `plan_codigo` + `modulos_activos` desde claims
- [ ] Test de integración: verificar que request sin `id_institucion` en JWT retorna 401

---

## HU-SE-02 — Gestionar planes de suscripción (5 SP)

**Objetivo:** Permitir al SUPER_ADMIN crear, editar y visualizar los planes del sistema.

### Base de datos
- [x] Tabla `plan_suscripcion` (en migración Sprint Especial)
- [x] Tabla `modulo_sistema` (en migración Sprint Especial)
- [x] Tabla `plan_modulo` (en migración Sprint Especial)
- [x] Seed de 3 planes + 9 módulos

### Backend
- [ ] Entidad `PlanSuscripcion.java` + `PlanSuscripcionRepository`
- [ ] Entidad `ModuloSistema.java` + `ModuloSistemaRepository`
- [ ] Entidad `PlanModulo.java` + `PlanModuloRepository`
- [ ] `PlanService` — CRUD completo, obtener módulos por plan
- [ ] `PlanController`:
  - `GET  /api/planes` — listar todos (público o SUPER_ADMIN)
  - `GET  /api/planes/{id}` — detalle + módulos incluidos
  - `POST /api/planes` — crear plan (SUPER_ADMIN)
  - `PUT  /api/planes/{id}` — actualizar (SUPER_ADMIN)
  - `PUT  /api/planes/{id}/modulos` — actualizar módulos del plan (SUPER_ADMIN)
- [ ] Registrar en `bitacora_auditoria` creación/modificación de planes
- [ ] `@PreAuthorize("hasRole('SUPER_ADMIN')")` en mutaciones

### Frontend
- [ ] Feature `features/sia/planes/` (solo visible para SUPER_ADMIN)
- [ ] Listado de planes con módulos incluidos (tabla + badges)
- [ ] Formulario crear/editar plan (modal)
- [ ] Panel de selección de módulos por plan (checkboxes)
- [ ] Conectar `plan.service.ts` con endpoints

---

## HU-SE-03 — Simular suscripción + validar límites (5 SP)

**Objetivo:** Permitir que una institución seleccione un plan, y que el sistema valide el límite de usuarios al registrar nuevos.

### Base de datos
- [x] Tabla `suscripcion_institucion` (en migración Sprint Especial)

### Backend
- [ ] Entidad `SuscripcionInstitucion.java` + `SuscripcionInstitucionRepository`
- [ ] `SuscripcionService`:
  - Suscribir institución a plan (valida que no haya otra ACTIVA)
  - Obtener plan activo de la institución actual
  - Obtener módulos activos de la institución
  - Validar `max_usuarios` antes de crear usuario
- [ ] `SuscripcionController`:
  - `GET  /api/suscripcion/activa` — plan actual de la institución del JWT
  - `POST /api/suscripcion` — suscribir (SUPER_ADMIN o ADMIN_INSTITUCION)
  - `PUT  /api/suscripcion/{id}/cancelar` — cancelar suscripción
- [ ] En `UsuarioService.crear()`: llamar `SuscripcionService.validarLimiteUsuarios()` → 409 si excede
- [ ] Registrar en `bitacora_auditoria` suscripción y cancelación

### Frontend
- [ ] Feature `features/sia/suscripcion/`
- [ ] Componente "Mi Plan": muestra plan activo, módulos, usuarios usados / máximo
- [ ] Flujo de simulación: selector de planes → confirmación → activar
- [ ] Badge de módulos disponibles según plan
- [ ] Conectar `suscripcion.service.ts`

---

## HU-SE-04 — Extender configuración paramétrica (2 SP)

**Objetivo:** Ampliar el uso de `ConfiguracionInstitucion` para que reglas de negocio variables se lean de configuración, no de código.

### Backend
- [ ] En `CalificacionService`: leer umbral de aprobación desde `ConfiguracionInstitucion` (clave: `UMBRAL_APROBACION`, default: `51`)
- [ ] En `AsistenciaService`: leer porcentaje mínimo de asistencia desde config (clave: `MIN_PORCENTAJE_ASISTENCIA`, default: `75`)
- [ ] Endpoint `GET /api/configuracion/catalogo` — lista claves estándar con descripción y valor por defecto
- [ ] Documentar en seed: insertar configuraciones por defecto si no existen

### Frontend
- [ ] Módulo `features/sia/configuracion/` ya existe: agregar sección "Reglas académicas"
- [ ] Campos: umbral de aprobación (número), porcentaje mínimo asistencia (número)
- [ ] Validar rango y tipo antes de guardar

---

## HU-SE-05 — CRUD roles dinámicos por institución (3 SP)

**Objetivo:** Permitir al ADMIN_INSTITUCION crear roles personalizados para su institución (además de los roles globales fijos).

> **Nota:** Las tablas `rol`, `permiso`, `rol_permiso` ya existen. Solo faltan endpoints CRUD para roles con `es_global=FALSE`.

### Backend
- [ ] `RolController` (si no existe): 
  - `GET  /api/roles` — listar roles globales + roles de la institución del JWT
  - `POST /api/roles` — crear rol con `es_global=FALSE, id_institucion=<del JWT>` (ADMIN_INSTITUCION)
  - `PUT  /api/roles/{id}` — actualizar nombre/descripción (solo roles propios de institución)
  - `DELETE /api/roles/{id}` — desactivar rol (solo si no tiene usuarios asignados)
- [ ] Validar en servicio: no permitir modificar roles globales (`es_global=TRUE`) desde institución
- [ ] Registrar en `bitacora_auditoria`

### Frontend
- [ ] Módulo `features/sia/roles/` ya existe: completar con creación/edición de roles dinámicos
- [ ] Indicar visualmente cuáles son globales (solo lectura) vs. propios de institución (editables)
- [ ] Formulario de creación de rol con nombre y descripción

---

## HU-SE-06 — UI asignación de permisos a roles (2 SP)

**Objetivo:** Permitir al ADMIN_INSTITUCION asignar y revocar permisos a los roles de su institución.

> **Nota:** Las tablas `permiso` y `rol_permiso` ya existen. El backend solo necesita los endpoints de asignación expuestos.

### Backend
- [ ] `GET  /api/permisos` — listar todos los permisos del sistema (agrupados por módulo)
- [ ] `GET  /api/roles/{id}/permisos` — obtener permisos actuales del rol
- [ ] `PUT  /api/roles/{id}/permisos` — reemplazar lista de permisos (body: lista de `id_permiso`)
- [ ] Registrar cambio de permisos en `bitacora_auditoria`
- [ ] Solo permitir asignar permisos a roles de la propia institución

### Frontend
- [ ] En módulo `roles/`: agregar pestaña/sección "Permisos del rol"
- [ ] Matriz de permisos por módulo (checkboxes agrupados)
- [ ] Guardar con confirmación
- [ ] Feedback visual de permisos guardados

---

## HU-SE-07 — Privilegios campo/botón por rol (8 SP)

**Objetivo:** Controlar visibilidad y editabilidad de campos y botones de formularios según el rol del usuario, sin duplicar formularios.

### Base de datos
- [x] Tabla `privilegio_ui` (en migración Sprint Especial)

### Backend
- [ ] Entidad `PrivilegioUi.java` + `PrivilegioUiRepository`
- [ ] `PrivilegioUiService`:
  - `obtenerPorRol(idInstitucion, idRol)` → Map<entidad, Map<campo, PrivilegioDto>>
  - `guardarPrivilegios(idInstitucion, idRol, List<PrivilegioUiRequest>)`
  - `obtenerPorUsuarioActual()` → construye mapa del usuario autenticado
- [ ] `PrivilegioUiController`:
  - `GET  /api/privilegios-ui` — mapa de privilegios del usuario autenticado (para el frontend)
  - `GET  /api/privilegios-ui/rol/{idRol}` — configuración por rol (ADMIN_INSTITUCION)
  - `PUT  /api/privilegios-ui/rol/{idRol}` — guardar configuración completa (ADMIN_INSTITUCION)
- [ ] Registrar cambios en `bitacora_auditoria`

### Frontend
- [ ] `AuthzService` en `core/services/`:
  ```typescript
  canView(modulo: string, entidad: string, campo: string): boolean
  canEdit(modulo: string, entidad: string, campo: string): boolean
  ```
  - Al hacer login, cargar mapa de privilegios desde `GET /api/privilegios-ui`
  - Guardar en signal o en memoria de sesión
- [ ] Directiva estructural `*appCanView="[modulo, entidad, campo]"` (oculta elemento)
- [ ] Directiva de atributo `[appCanEdit]="[modulo, entidad, campo]"` (deshabilita input)
- [ ] **Demo:** aplicar en formulario de `estudiantes/` — ocultar `documento_identidad` para rol DOCENTE; campo `correo` solo lectura para SECRETARIO
- [ ] Panel de administración de privilegios UI (en módulo `roles/` o sección propia):
  - Selector de rol
  - Tabla por entidad con tres estados por campo: Editable / Solo lectura / Oculto

---

## HU-SE-08 — Consulta de bitácora con filtros (3 SP)

**Objetivo:** Exponer la bitácora de auditoría (ya registrada) como endpoint consultable con filtros.

> **Nota:** `BitacoraAuditoria` y `AuditoriaService` ya existen y se usan en ~40 services.

### Backend
- [ ] `AuditoriaController`:
  - `GET /api/auditoria` — listar con filtros paginados:
    - `?idUsuario=`, `?nombreModulo=`, `?tipoOperacion=`, `?fechaDesde=`, `?fechaHasta=`, `?exito=`
  - Seguridad: `SUPER_ADMIN` ve toda la plataforma; `ADMIN_INSTITUCION`/`DIRECTOR` solo su institución
- [ ] `AuditoriaResponse` DTO (subconjunto de campos — no exponer `hash_integridad` en listado)
- [ ] Paginación con `Pageable` (Spring Data)

### Frontend
- [ ] Módulo `features/sia/auditoria/` ya existe: completar con vista de listado
- [ ] Filtros: módulo (dropdown), operación, fecha desde/hasta, usuario, éxito/fallo
- [ ] Tabla con columnas: fecha, usuario, módulo, operación, éxito, mensaje
- [ ] Paginación del lado del servidor

---

## HU-SE-09 — Intentos de login fallidos + consulta (3 SP)

**Objetivo:** Registrar cada intento de autenticación (exitoso y fallido) en `intento_login` y proveer endpoint de consulta.

### Base de datos
- [x] Tabla `intento_login` (en migración Sprint Especial)

### Backend
- [ ] Entidad `IntentoLogin.java` + `IntentoLoginRepository`
- [ ] `IntentoLoginService.registrar(correo, exito, ip, agente, motivoFallo)`
- [ ] En `AuthService.login()`:
  - Login exitoso → `intentoLoginService.registrar(..., true, null)`
  - Login fallido (catch) → `intentoLoginService.registrar(..., false, motivo)` → **NO lanzar acá**, registrar y luego relanzar la excepción
- [ ] `IntentoLoginController`:
  - `GET /api/intentos-login` — filtros: `?correo=`, `?solo_fallidos=`, `?fechaDesde=`, `?fechaHasta=` (SUPER_ADMIN o ADMIN_INSTITUCION)
- [ ] Detectar múltiples fallos: si >5 intentos fallidos en 15 min para el mismo correo → registrar alerta en `bitacora_auditoria`

### Frontend
- [ ] Sección en módulo `auditoria/` o sección propia: "Intentos de acceso"
- [ ] Filtro de solo fallidos (toggle)
- [ ] Tabla: fecha, correo, IP, motivo, éxito/fallo
- [ ] Badge de advertencia si hay correo con múltiples fallos recientes

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
- [ ] `RespaldoController`:
  - `GET  /api/respaldos` — historial (filtros por institución, tipo, estado)
  - `POST /api/respaldos` — iniciar backup (SUPER_ADMIN o ADMIN_INSTITUCION con plan EMPRESARIAL)
  - `GET  /api/restauraciones` — historial de solicitudes
  - `POST /api/restauraciones` — solicitar restauración
  - `PUT  /api/restauraciones/{id}/aprobar` — aprobar (solo SUPER_ADMIN)
- [ ] Registrar en `bitacora_auditoria`

### Frontend
- [ ] Feature `features/sia/backups/`:
  - Lista de backups con estado (badge color)
  - Botón "Iniciar backup" (según permisos del plan)
  - Lista de solicitudes de restauración
  - Formulario solicitud de restauración (selector de backup + motivo)
  - Para SUPER_ADMIN: botón aprobar/rechazar restauración
- [ ] Conectar `respaldo.service.ts`

---

## HU-SE-11 — Reportes con filtros previos (5 SP)

**Objetivo:** Todo reporte debe tener una pantalla previa de filtros. El usuario define parámetros antes de generar.

### Base de datos
- [x] Tabla `bitacora_reporte` (para auditoría, en migración Sprint Especial)

### Backend
- [ ] `ReporteController`:
  - `GET /api/reportes/asistencia` — filtros: `idGestion`, `idParalelo`, `idEstudiante`, `fechaDesde`, `fechaHasta`, `estadoAsistencia`
  - `GET /api/reportes/calificaciones` — filtros: `idGestion`, `idParalelo`, `idMateria`, `idEstudiante`, `tipoEvaluacion`
  - `GET /api/reportes/inscripciones` — filtros: `idGestion`, `idCurso`, `idParalelo`, `estado`
- [ ] Todos los endpoints registran en `bitacora_reporte` al ser invocados
- [ ] Paginación opcional (con/sin página)

### Frontend
- [ ] Feature `features/sia/reportes/`:
  - Página de selección de tipo de reporte
  - Panel de filtros específico por tipo (usando PrimeNG `p-panel` plegable)
  - Tabla de resultados (solo al pulsar "Generar")
  - Botón "Limpiar filtros"
- [ ] Conectar `reporte.service.ts`

---

## HU-SE-12 — Reportes analíticos y gerenciales (5 SP)

**Objetivo:** Reportes detallados (analíticos) y reportes resumen (gerenciales/estratégicos).

### Backend
- [ ] Reporte analítico de asistencia: detalle por sesión, estudiante, estado
- [ ] Reporte analítico de calificaciones: detalle por evaluación, nota por estudiante
- [ ] Reporte gerencial de asistencia: % de asistencia por paralelo/materia, ranking estudiantes
- [ ] Reporte gerencial de calificaciones: promedio por paralelo/materia, distribución de notas
- [ ] Todos retornan estructura unificada `ReporteResponse<T>` con metadatos y `List<T> datos`

### Frontend
- [ ] En módulo `reportes/`: dos pestañas "Analítico" / "Gerencial"
- [ ] Reporte gerencial: cards de KPI (% asistencia promedio, nota promedio) + tabla resumen
- [ ] Reporte analítico: tabla detallada con scroll virtual (PrimeNG `p-table` lazy)
- [ ] Posibilidad de exportar como CSV (tabla HTML + download)

---

## HU-SE-13 — Reporte dinámico configurable (8 SP)

**Objetivo:** Permitir al usuario seleccionar columnas y filtros antes de generar. El Admin puede definir plantillas reutilizables.

### Base de datos
- [x] Tabla `reporte_configurable` (en migración Sprint Especial)
- [x] Tabla `reporte_campo` (en migración Sprint Especial)

### Backend
- [ ] Entidades `ReporteConfigurable.java`, `ReporteCampo.java` + Repositories
- [ ] `ReporteService`:
  - `listarPlantillas(idInstitucion)` → plantillas propias + globales
  - `crearPlantilla(request)` — ADMIN_INSTITUCION
  - `generarReporte(idReporte, filtros, camposSeleccionados)` → consulta dinámica con JPA Criteria o QueryDSL
  - Registrar en `bitacora_reporte`
- [ ] `ReporteController`:
  - `GET  /api/reportes/plantillas` — listar plantillas disponibles
  - `POST /api/reportes/plantillas` — crear plantilla (ADMIN_INSTITUCION)
  - `POST /api/reportes/generar/{idReporte}` — generar con filtros seleccionados
  - `GET  /api/reportes/campos/{entidad}` — campos disponibles de una entidad para UI

### Frontend
- [ ] Sub-vista "Reportes dinámicos" en módulo `reportes/`:
  - Selector de plantilla (o crear nueva)
  - Panel de selección de columnas (checkboxes por campo del `reporte_campo`)
  - Panel de filtros dinámicos (solo los marcados como `es_filtro=true`)
  - Tabla resultado con columnas elegidas
  - Guardar como nueva plantilla (nombre + descripción)
- [ ] Vista de administración de plantillas (ADMIN_INSTITUCION/SUPER_ADMIN):
  - CRUD de plantillas y sus campos

---

## Limpieza técnica (sin SP asignados — hacer antes de iniciar)

### Backend
- [ ] Verificar que todos los `@PreAuthorize` sean correctos (no endpoints públicos que debieran ser privados)
- [ ] Agregar `CONSTRAINT uq_rol_id_institucion UNIQUE (id, id_institucion)` a tabla `rol` si se necesita FK compuesta para `privilegio_ui`

### Frontend
- [ ] Eliminar `ROLE_PERMISSION_FALLBACK` hardcoded en `auth.service.ts` (líneas 18-58)
- [ ] Limpiar endpoints legacy en `http-api.ts` (`catalog/`, `sales/`, `analyticsReports`)
- [ ] Agregar en `http-api.ts` los nuevos endpoints:
  ```typescript
  static planes          = 'planes/';
  static suscripcion     = 'suscripcion/';
  static privilegiosUi   = 'privilegios-ui/';
  static intentosLogin   = 'intentos-login/';
  static respaldos       = 'respaldos/';
  static restauraciones  = 'restauraciones/';
  static reportes        = 'reportes/';
  ```
- [ ] Agregar ítems en `menu.service.ts` para SUPER_ADMIN: Planes, Suscripciones, Respaldos
- [ ] Agregar ítems para ADMIN_INSTITUCION: Reportes, Backups (según plan activo)
