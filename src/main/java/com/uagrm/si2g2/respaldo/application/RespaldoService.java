package com.uagrm.si2g2.respaldo.application;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RespaldoService {

    /** Tablas de negocio que contienen datos por institución */
    private static final List<String> TABLAS_TENANT = List.of(
            "gestion_academica", "curso", "paralelo", "materia", "curso_materia",
            "docente", "estudiante", "tutor", "estudiante_tutor",
            "inscripcion", "asignacion_docente",
            "aula", "horario",
            "sesion_asistencia", "registro_asistencia",
            "tipo_evaluacion", "evaluacion", "registro_calificacion"
    );

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final S3Client s3Client;
    private final AppProperties appProperties;
    private final RegistroRespaldoRepository respaldoRepository;
    private final RegistroRestauracionRepository restauracionRepository;

    // -------------------------------------------------------------------------
    // RESPALDOS
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<RegistroRespaldo> listarRespaldos(UUID idInstitucion) {
        if (idInstitucion != null) {
            return respaldoRepository.findAllByIdInstitucionOrderByFechaInicioDesc(idInstitucion);
        }
        return respaldoRepository.findAllByOrderByFechaInicioDesc();
    }

    /**
     * Inicia un respaldo real por tenant: exporta todas las tablas de negocio
     * para la institución dada y sube el resultado como JSON a S3.
     */
    @Transactional
    public RegistroRespaldo iniciarRespaldo(UUID idInstitucion) {
        UUID usuarioActual = SecurityUtils.currentUserId();

        // 1. Crear registro en estado EN_PROGRESO
        RegistroRespaldo registro = new RegistroRespaldo();
        registro.setIdInstitucion(idInstitucion);
        registro.setTipoRespaldo("POR_TENANT");
        registro.setEstado("EN_PROGRESO");
        registro.setIniciadoPor(usuarioActual);
        registro.setFechaInicio(OffsetDateTime.now());
        registro.setSimulado(false);
        registro = respaldoRepository.save(registro);

        try {
            // 2. Exportar datos de cada tabla para la institución
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

            // 3. Serializar a JSON
            byte[] contenido = objectMapper.writeValueAsBytes(Map.of(
                    "idInstitucion", idInstitucion.toString(),
                    "fechaRespaldo", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now()),
                    "tablas", datos
            ));

            // 4. Subir a S3
            String s3Key = String.format("backups/%s/%s.json",
                    idInstitucion,
                    DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(OffsetDateTime.now()));
            String bucket = appProperties.getAws().getS3().getBucket();

            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(s3Key)
                            .contentType("application/json")
                            .build(),
                    RequestBody.fromBytes(contenido)
            );

            // 5. Actualizar registro como COMPLETADO
            registro.setEstado("COMPLETADO");
            registro.setFechaFin(OffsetDateTime.now());
            registro.setRutaAlmacenamiento(s3Key);
            registro.setTamanioBytes((long) contenido.length);

        } catch (Exception e) {
            log.error("[RespaldoService] Error al realizar respaldo para institución {}: {}", idInstitucion, e.getMessage(), e);
            registro.setEstado("FALLIDO");
            registro.setFechaFin(OffsetDateTime.now());
            registro.setObservacion("Error: " + e.getMessage());
        }

        return respaldoRepository.save(registro);
    }

    // -------------------------------------------------------------------------
    // RESTAURACIONES
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<RegistroRestauracion> listarRestauraciones(UUID idInstitucion) {
        if (idInstitucion != null) {
            return restauracionRepository.findAllByIdInstitucionOrderByFechaSolicitudDesc(idInstitucion);
        }
        return restauracionRepository.findAllByOrderByFechaSolicitudDesc();
    }

    @Transactional
    public RegistroRestauracion solicitarRestauracion(UUID idRespaldo, String motivo) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        UUID usuarioActual = SecurityUtils.currentUserId();

        RegistroRespaldo respaldo = respaldoRepository.findById(idRespaldo)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Respaldo no encontrado: " + idRespaldo));

        // Validar que el respaldo pertenece a la misma institución
        if (!idInstitucion.equals(respaldo.getIdInstitucion())) {
            throw new AccessDeniedException("No tiene acceso a ese respaldo");
        }

        RegistroRestauracion solicitud = new RegistroRestauracion();
        solicitud.setIdRespaldo(idRespaldo);
        solicitud.setIdInstitucion(idInstitucion);
        solicitud.setSolicitadoPor(usuarioActual);
        solicitud.setMotivo(motivo);
        solicitud.setEstado("PENDIENTE");
        solicitud.setSimulado(false);

        return restauracionRepository.save(solicitud);
    }

    @Transactional
    public RegistroRestauracion aprobarRestauracion(UUID idRestauracion) {
        UUID aprobadorId = SecurityUtils.currentUserId();

        RegistroRestauracion solicitud = restauracionRepository.findById(idRestauracion)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Solicitud no encontrada: " + idRestauracion));

        if (!"PENDIENTE".equals(solicitud.getEstado())) {
            throw new IllegalStateException("La solicitud no está en estado PENDIENTE");
        }

        solicitud.setEstado("APROBADO");
        solicitud.setAprobadoPor(aprobadorId);
        solicitud.setFechaEjecucion(OffsetDateTime.now());

        return restauracionRepository.save(solicitud);
    }
}
