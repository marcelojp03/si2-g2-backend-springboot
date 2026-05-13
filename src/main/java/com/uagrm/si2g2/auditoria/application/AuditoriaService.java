package com.uagrm.si2g2.auditoria.application;

import com.uagrm.si2g2.auditoria.domain.BitacoraAuditoria;
import com.uagrm.si2g2.auditoria.domain.BitacoraAuditoriaRepository;
import com.uagrm.si2g2.config.AppProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final BitacoraAuditoriaRepository bitacoraAuditoriaRepository;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;

    public void registrar(UUID idInstitucion, UUID idUsuario, String nombreModulo,
                          String tipoOperacion, String nombreEntidad, String idEntidad,
                          boolean exito, String mensaje) {
        registrarDetallado(idInstitucion, idUsuario, nombreModulo, tipoOperacion, nombreEntidad,
                idEntidad, null, null, exito, mensaje);
    }

    public void registrarDetallado(UUID idInstitucion, UUID idUsuario, String nombreModulo,
                                   String tipoOperacion, String nombreEntidad, String idEntidad,
                                   Object datosAntes, Object datosDespues,
                                   boolean exito, String mensaje) {
        try {
            RequestAuditContext requestContext = resolveRequestContext();
            String jsonAntes = toJson(datosAntes);
            String jsonDespues = toJson(datosDespues);

            BitacoraAuditoria registro = BitacoraAuditoria.builder()
                    .idInstitucion(idInstitucion)
                    .idUsuario(idUsuario)
                    .fechaEvento(Instant.now())
                    .direccionIp(requestContext.getIp())
                    .plataformaCliente(requestContext.getPlatform())
                    .agenteUsuario(requestContext.getUserAgent())
                    .metodoHttp(requestContext.getMetodoHttp())
                    .rutaRecurso(requestContext.getRutaRecurso())
                    .nombreModulo(nombreModulo)
                    .nombreFuncion(buildFunctionName(nombreModulo, tipoOperacion, requestContext))
                    .nombreEntidad(nombreEntidad)
                    .idEntidad(idEntidad)
                    .tipoOperacion(tipoOperacion)
                    .datosAntes(jsonAntes)
                    .datosDespues(jsonDespues)
                    .exito(exito)
                    .mensaje(mensaje)
                    .hashIntegridad(buildIntegrityHash(idInstitucion, idUsuario, nombreModulo, tipoOperacion,
                            nombreEntidad, idEntidad, requestContext, jsonAntes, jsonDespues, exito, mensaje))
                    .build();
            bitacoraAuditoriaRepository.save(registro);
        } catch (Exception e) {
            log.warn("No se pudo guardar registro de auditoría [{}:{}]: {}",
                    nombreModulo, tipoOperacion, e.getMessage());
        }
    }

    private RequestAuditContext resolveRequestContext() {
        String ip = "desconocida";
        String agenteUsuario = null;
        String metodoHttp = null;
        String rutaRecurso = null;

        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attrs.getRequest();
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            ip = (xForwardedFor != null && !xForwardedFor.isBlank())
                    ? xForwardedFor.split(",")[0].trim()
                    : request.getRemoteAddr();
            agenteUsuario = request.getHeader("User-Agent");
            metodoHttp = request.getMethod();
            rutaRecurso = request.getRequestURI();
        } catch (IllegalStateException ignored) {
        }

        return RequestAuditContext.builder()
                .ip(ip)
                .userAgent(agenteUsuario)
                .platform(resolvePlatform(agenteUsuario))
                .metodoHttp(metodoHttp)
                .rutaRecurso(rutaRecurso)
                .build();
    }

    private String resolvePlatform(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return null;
        }
        String normalized = userAgent.toLowerCase();
        if (normalized.contains("android")) return "ANDROID";
        if (normalized.contains("iphone") || normalized.contains("ipad") || normalized.contains("ios")) return "IOS";
        if (normalized.contains("windows")) return "WINDOWS";
        if (normalized.contains("mac os") || normalized.contains("macintosh")) return "MAC";
        if (normalized.contains("linux")) return "LINUX";
        return "WEB";
    }

    private String buildFunctionName(String modulo, String tipoOperacion, RequestAuditContext requestContext) {
        String ruta = requestContext.getRutaRecurso() != null ? requestContext.getRutaRecurso() : "sin-ruta";
        String metodo = requestContext.getMetodoHttp() != null ? requestContext.getMetodoHttp() : "sin-metodo";
        return modulo + ":" + tipoOperacion + "@" + metodo + " " + ruta;
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String stringValue) {
            return stringValue;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String buildIntegrityHash(UUID idInstitucion, UUID idUsuario, String modulo, String tipoOperacion,
                                      String nombreEntidad, String idEntidad, RequestAuditContext requestContext,
                                      String datosAntes, String datosDespues, boolean exito, String mensaje) {
        String payload = String.join("|",
                safe(appProperties.getSecurity().getAudit().getHashSecret()),
                safe(idInstitucion),
                safe(idUsuario),
                safe(modulo),
                safe(tipoOperacion),
                safe(nombreEntidad),
                safe(idEntidad),
                safe(requestContext.getIp()),
                safe(requestContext.getMetodoHttp()),
                safe(requestContext.getRutaRecurso()),
                safe(datosAntes),
                safe(datosDespues),
                Boolean.toString(exito),
                safe(mensaje)
        );
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return null;
        }
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
