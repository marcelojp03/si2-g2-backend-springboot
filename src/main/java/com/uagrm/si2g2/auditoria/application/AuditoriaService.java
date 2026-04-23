package com.uagrm.si2g2.auditoria.application;

import com.uagrm.si2g2.auditoria.domain.BitacoraAuditoria;
import com.uagrm.si2g2.auditoria.domain.BitacoraAuditoriaRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final BitacoraAuditoriaRepository bitacoraAuditoriaRepository;

    public void registrar(UUID idInstitucion, UUID idUsuario, String nombreModulo,
                          String tipoOperacion, String nombreEntidad, String idEntidad,
                          boolean exito, String mensaje) {
        try {
            String ip = "desconocida";
            String agenteUsuario = null;
            try {
                ServletRequestAttributes attrs =
                        (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
                HttpServletRequest request = attrs.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                ip = (xForwardedFor != null && !xForwardedFor.isBlank())
                        ? xForwardedFor.split(",")[0].trim()
                        : request.getRemoteAddr();
                agenteUsuario = request.getHeader("User-Agent");
            } catch (IllegalStateException ignored) { }

            BitacoraAuditoria registro = BitacoraAuditoria.builder()
                    .idInstitucion(idInstitucion)
                    .idUsuario(idUsuario)
                    .fechaEvento(Instant.now())
                    .direccionIp(ip)
                    .agenteUsuario(agenteUsuario)
                    .nombreModulo(nombreModulo)
                    .nombreEntidad(nombreEntidad)
                    .idEntidad(idEntidad)
                    .tipoOperacion(tipoOperacion)
                    .exito(exito)
                    .mensaje(mensaje)
                    .build();
            bitacoraAuditoriaRepository.save(registro);
        } catch (Exception e) {
            log.warn("No se pudo guardar registro de auditoría [{}:{}]: {}",
                    nombreModulo, tipoOperacion, e.getMessage());
        }
    }
}
