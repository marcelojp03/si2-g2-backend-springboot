package com.uagrm.si2g2.respaldo.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uagrm.si2g2.auditoria.application.AuditoriaService;
import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.config.AppProperties;
import com.uagrm.si2g2.respaldo.domain.RegistroRespaldo;
import com.uagrm.si2g2.respaldo.domain.RegistroRespaldoRepository;
import com.uagrm.si2g2.respaldo.domain.RegistroRestauracion;
import com.uagrm.si2g2.respaldo.domain.RegistroRestauracionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RespaldoService {

    private static final List<String> TABLAS_TENANT = List.of(
            "gestion_academica", "curso", "paralelo", "materia", "curso_materia",
            "docente", "estudiante", "tutor", "estudiante_tutor",
            "inscripcion", "asignacion_docente",
            "aula", "horario_clase",
            "asistencia_registro",
            "evaluacion", "evaluacion_materia", "calificacion", "calificacion_actividad",
            "calificacion_ser", "calificacion_cambio",
            "actividad_evaluativa", "periodo_evaluacion", "dimension",
            "autoevaluacion_trimestral", "observacion_ser",
            "comunicado", "notificacion",
            "pago", "cuota_estudiante",
            "alerta_riesgo",
            "reporte_configurable", "reporte_dashboard_widget", "reporte_ejecucion", "reporte_filtro_favorito",
            "solicitud_eliminacion_dimension"
    );

    private static final String KEY_PATTERN = "backups/%s/%s.json";
    private static final String DATE_PATTERN = "yyyyMMdd_HHmmss";

    private static final Set<String> TABLAS_SIN_ID = Set.of(
            "asistencia_registro", "calificacion_cambio", "observacion_ser"
    );

    private static final Map<String, String> PK_COLUMNS = Map.ofEntries(
            Map.entry("gestion_academica", "id"),
            Map.entry("curso", "id"),
            Map.entry("paralelo", "id"),
            Map.entry("materia", "id"),
            Map.entry("curso_materia", "id"),
            Map.entry("docente", "id"),
            Map.entry("estudiante", "id"),
            Map.entry("tutor", "id"),
            Map.entry("estudiante_tutor", "id"),
            Map.entry("inscripcion", "id"),
            Map.entry("asignacion_docente", "id"),
            Map.entry("aula", "id"),
            Map.entry("horario_clase", "id"),
            Map.entry("asistencia_registro", "id"),
            Map.entry("evaluacion", "id"),
            Map.entry("evaluacion_materia", "id"),
            Map.entry("calificacion", "id"),
            Map.entry("calificacion_actividad", "id"),
            Map.entry("calificacion_ser", "id"),
            Map.entry("calificacion_cambio", "id"),
            Map.entry("actividad_evaluativa", "id"),
            Map.entry("periodo_evaluacion", "id"),
            Map.entry("dimension", "id"),
            Map.entry("autoevaluacion_trimestral", "id"),
            Map.entry("observacion_ser", "id"),
            Map.entry("comunicado", "id"),
            Map.entry("notificacion", "id"),
            Map.entry("pago", "id"),
            Map.entry("cuota_estudiante", "id"),
            Map.entry("alerta_riesgo", "id"),
            Map.entry("reporte_configurable", "id"),
            Map.entry("reporte_dashboard_widget", "id"),
            Map.entry("reporte_ejecucion", "id"),
            Map.entry("reporte_filtro_favorito", "id"),
            Map.entry("solicitud_eliminacion_dimension", "id")
    );

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final S3Client s3Client;
    private final AppProperties appProperties;
    private final AuditoriaService auditoriaService;
    private final RegistroRespaldoRepository respaldoRepository;
    private final RegistroRestauracionRepository restauracionRepository;

    @Transactional(readOnly = true)
    public List<RegistroRespaldo> listarRespaldos(UUID idInstitucion) {
        if (idInstitucion != null) {
            return respaldoRepository.findAllByIdInstitucionOrderByFechaInicioDesc(idInstitucion);
        }
        return respaldoRepository.findAllByOrderByFechaInicioDesc();
    }

    @Transactional(noRollbackFor = Exception.class)
    public RegistroRespaldo iniciarRespaldo(UUID idInstitucion) {
        UUID usuarioActual = SecurityUtils.currentUserId();
        RegistroRespaldo registro = new RegistroRespaldo();
        registro.setIdInstitucion(idInstitucion);
        registro.setTipoRespaldo("POR_TENANT");
        registro.setEstado("EN_PROGRESO");
        registro.setIniciadoPor(usuarioActual);
        registro.setFechaInicio(OffsetDateTime.now());
        registro.setSimulado(false);
        registro = respaldoRepository.save(registro);

        try {
            Map<String, List<Map<String, Object>>> datos = new LinkedHashMap<>();
            for (String tabla : TABLAS_TENANT) {
                try {
                    List<Map<String, Object>> filas = jdbc.queryForList(
                            "SELECT * FROM sia." + tabla + " WHERE id_institucion = ?",
                            idInstitucion
                    );
                    datos.put(tabla, filas);
                } catch (Exception e) {
                    log.warn("[RespaldoService] Tabla '{}' no encontrada o sin id_institucion, omitida: {}", tabla, e.getMessage());
                    datos.put(tabla, List.of());
                }
            }

            byte[] contenido = objectMapper.writeValueAsBytes(Map.of(
                    "idInstitucion", idInstitucion.toString(),
                    "fechaRespaldo", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now()),
                    "tablas", datos
            ));

            String s3Key = generarS3Key(idInstitucion);
            String bucket = appProperties.getAws().getS3().getBucket();

            s3Client.putObject(
                    PutObjectRequest.builder().bucket(bucket).key(s3Key).contentType("application/json").build(),
                    RequestBody.fromBytes(contenido)
            );

            registro.setEstado("COMPLETADO");
            registro.setFechaFin(OffsetDateTime.now());
            registro.setRutaAlmacenamiento(s3Key);
            registro.setTamanioBytes((long) contenido.length);

            auditoriaService.registrar(idInstitucion, usuarioActual, "RESPALDO", "CREAR",
                    "registro_respaldo", registro.getId().toString(), true,
                    "Respaldo " + registro.getTipoRespaldo() + " completado");
            log.info("[RespaldoService] Respaldo {} completado para institución {}: {} tablas, {} bytes",
                    registro.getTipoRespaldo(), idInstitucion, datos.size(), contenido.length);

        } catch (Exception e) {
            log.error("[RespaldoService] Error al realizar respaldo para institución {}: {}", idInstitucion, e.getMessage(), e);
            registro.setEstado("FALLIDO");
            registro.setFechaFin(OffsetDateTime.now());
            registro.setObservacion("Error: " + e.getMessage());
            auditoriaService.registrar(idInstitucion, usuarioActual, "RESPALDO", "ERROR",
                    "registro_respaldo", registro.getId().toString(), false,
                    "Respaldo fallido: " + e.getMessage());
        }

        return respaldoRepository.save(registro);
    }

    @Transactional(noRollbackFor = Exception.class)
    public RegistroRestauracion solicitarRestauracion(UUID idRespaldo, String motivo) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        UUID usuarioActual = SecurityUtils.currentUserId();

        RegistroRespaldo respaldo = respaldoRepository.findById(idRespaldo)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Respaldo no encontrado: " + idRespaldo));

        if (!idInstitucion.equals(respaldo.getIdInstitucion())) {
            throw new AccessDeniedException("No tiene acceso a ese respaldo");
        }

        // 1. Auto-snapshot antes de restaurar
        RegistroRespaldo snapshot = null;
        try {
            snapshot = iniciarRespaldo(idInstitucion);
            log.info("[RespaldoService] Snapshot pre-restauración creado: {}", snapshot.getId());
            auditoriaService.registrar(idInstitucion, usuarioActual, "RESPALDO", "SNAPSHOT",
                    "registro_respaldo", snapshot.getId().toString(), true,
                    "Snapshot pre-restauración automático");
        } catch (Exception e) {
            log.error("[RespaldoService] Error al crear snapshot pre-restauración", e);
        }

        // 2. Crear solicitud con auto-aprobación para ADMIN_INSTITUCION
        boolean autoAprobar = SecurityUtils.currentUserHasRole("ADMIN_INSTITUCION")
                || SecurityUtils.currentUserHasRole("SUPER_ADMIN");

        RegistroRestauracion solicitud = new RegistroRestauracion();
        solicitud.setIdRespaldo(idRespaldo);
        solicitud.setIdInstitucion(idInstitucion);
        solicitud.setSolicitadoPor(usuarioActual);
        solicitud.setMotivo(motivo);
        solicitud.setSimulado(false);

        if (autoAprobar) {
            solicitud.setEstado("APROBADO");
            solicitud.setAprobadoPor(usuarioActual);
            solicitud.setFechaEjecucion(OffsetDateTime.now());
        } else {
            solicitud.setEstado("PENDIENTE");
        }

        solicitud = restauracionRepository.save(solicitud);

        // 3. Ejecutar restauración si está aprobada
        if (autoAprobar) {
            ejecutarRestauracion(solicitud, respaldo, snapshot);
        }

        return solicitud;
    }

    @Transactional
    public RegistroRestauracion aprobarRestauracion(UUID idRestauracion) {
        RegistroRestauracion solicitud = restauracionRepository.findById(idRestauracion)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Solicitud no encontrada: " + idRestauracion));

        if (!"PENDIENTE".equals(solicitud.getEstado())) {
            throw new IllegalStateException("La solicitud no está en estado PENDIENTE");
        }

        UUID idInstitucion = solicitud.getIdInstitucion();

        // Snapshot antes de restaurar
        RegistroRespaldo snapshot = null;
        try {
            snapshot = iniciarRespaldo(idInstitucion);
            log.info("[RespaldoService] Snapshot pre-restauración creado: {}", snapshot.getId());
        } catch (Exception e) {
            log.error("[RespaldoService] Error al crear snapshot pre-restauración", e);
        }

        solicitud.setEstado("APROBADO");
        solicitud.setAprobadoPor(SecurityUtils.currentUserId());
        solicitud.setFechaEjecucion(OffsetDateTime.now());
        solicitud = restauracionRepository.save(solicitud);

        RegistroRespaldo respaldo = respaldoRepository.findById(solicitud.getIdRespaldo())
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Respaldo no encontrado"));

        ejecutarRestauracion(solicitud, respaldo, snapshot);

        return solicitud;
    }

    private void ejecutarRestauracion(RegistroRestauracion solicitud, RegistroRespaldo respaldo, RegistroRespaldo snapshot) {
        UUID idInstitucion = solicitud.getIdInstitucion();

        try {
            // Leer backup desde S3
            String bucket = appProperties.getAws().getS3().getBucket();
            ResponseBytes<?> s3Object = s3Client.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(bucket).key(respaldo.getRutaAlmacenamiento()).build()
            );

            Map<String, Object> backupData = objectMapper.readValue(s3Object.asByteArray(), new TypeReference<>() {});
            Map<String, List<Map<String, Object>>> tablas = (Map<String, List<Map<String, Object>>>) backupData.get("tablas");

            int totalInsertados = 0;
            int totalActualizados = 0;
            List<String> errores = new ArrayList<>();

            for (String tabla : TABLAS_TENANT) {
                List<Map<String, Object>> filas = tablas.getOrDefault(tabla, List.of());
                if (filas.isEmpty()) continue;

                String pk = PK_COLUMNS.getOrDefault(tabla, "id");

                for (Map<String, Object> fila : filas) {
                    try {
                        Map<String, Object> cols = new LinkedHashMap<>(fila);
                        cols.remove("creado_en");
                        cols.remove("actualizado_en");

                        String idValor = cols.containsKey(pk) ? "'" + cols.get(pk) + "'::uuid" : "gen_random_uuid()";
                        String setClause = cols.entrySet().stream()
                                .filter(e -> !e.getKey().equals(pk))
                                .map(e -> {
                                    Object v = e.getValue();
                                    if (v == null) return e.getKey() + " = NULL";
                                    if (v instanceof Number) return e.getKey() + " = " + v;
                                    return e.getKey() + " = '" + v.toString().replace("'", "''") + "'::" + inferType(v);
                                })
                                .collect(Collectors.joining(", "));

                        String colNames = String.join(", ", cols.keySet());
                        String colVals = cols.values().stream()
                                .map(v -> {
                                    if (v == null) return "NULL";
                                    if (v instanceof Number) return v.toString();
                                    return "'" + v.toString().replace("'", "''") + "'::" + inferType(v);
                                })
                                .collect(Collectors.joining(", "));

                        String sql = String.format(
                                "INSERT INTO sia.%s (%s) VALUES (%s) ON CONFLICT (%s) DO UPDATE SET %s",
                                tabla, colNames, colVals, pk, setClause
                        );

                        int affected = jdbc.update(sql);
                        if (affected == 1) totalInsertados++;
                        else totalActualizados++;
                    } catch (Exception e) {
                        errores.add(tabla + ": " + e.getMessage());
                    }
                }
            }

            solicitud.setResultado(String.format(
                    "Restauración completada: %d insertados, %d actualizados, %d errores",
                    totalInsertados, totalActualizados, errores.size()
            ));

            auditoriaService.registrar(idInstitucion, solicitud.getSolicitadoPor(), "RESTAURACION", "EJECUTAR",
                    "registro_restauracion", solicitud.getId().toString(), errores.isEmpty(),
                    String.format("Restauración de respaldo %s: %d insertados, %d actualizados, %d errores",
                            respaldo.getId(), totalInsertados, totalActualizados, errores.size()));
            log.info("[RespaldoService] Restauración {} completada: {} insertados, {} actualizados, {} errores",
                    solicitud.getId(), totalInsertados, totalActualizados, errores.size());

            if (!errores.isEmpty()) {
                log.warn("[RespaldoService] Errores durante restauración: {}", String.join("; ", errores));
            }

        } catch (Exception e) {
            log.error("[RespaldoService] Error durante restauración: {}", e.getMessage(), e);
            solicitud.setResultado("Error: " + e.getMessage());
            auditoriaService.registrar(idInstitucion, solicitud.getSolicitadoPor(), "RESTAURACION", "ERROR",
                    "registro_restauracion", solicitud.getId().toString(), false,
                    "Restauración fallida: " + e.getMessage());

            if (snapshot != null) {
                solicitud.setResultado(solicitud.getResultado()
                        + " | Snapshot disponible: " + snapshot.getId());
            }
        }
    }

    private String generarS3Key(UUID idInstitucion) {
        return String.format(KEY_PATTERN,
                idInstitucion,
                DateTimeFormatter.ofPattern(DATE_PATTERN).format(OffsetDateTime.now()));
    }

    private String inferType(Object v) {
        if (v instanceof Boolean) return "boolean";
        if (v instanceof Integer) return "integer";
        if (v instanceof Long) return "bigint";
        if (v instanceof Double || v instanceof Float) return "numeric";
        if (v.toString().matches("^\\d{4}-\\d{2}-\\d{2}(T| ).*")) return "timestamp with time zone";
        if (v.toString().matches("^\\d{4}-\\d{2}-\\d{2}$")) return "date";
        return "text";
    }

    @Transactional(readOnly = true)
    public List<RegistroRestauracion> listarRestauraciones(UUID idInstitucion) {
        if (idInstitucion != null) {
            return restauracionRepository.findAllByIdInstitucionOrderByFechaSolicitudDesc(idInstitucion);
        }
        return restauracionRepository.findAllByOrderByFechaSolicitudDesc();
    }
}
