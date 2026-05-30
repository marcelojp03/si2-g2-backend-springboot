package com.uagrm.si2g2.auth.application;

import com.uagrm.si2g2.auditoria.application.AuditoriaService;
import com.uagrm.si2g2.auth.domain.IntentoLogin;
import com.uagrm.si2g2.auth.domain.IntentoLoginRepository;
import com.uagrm.si2g2.auth.domain.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntentoLoginService {

    /** Umbral de fallos consecutivos antes de generar alerta de seguridad. */
    private static final int UMBRAL_FALLOS = 5;
    /** Ventana de tiempo en minutos para contar fallos. */
    private static final int VENTANA_MINUTOS = 15;

    private final IntentoLoginRepository intentoRepo;
    private final AuditoriaService auditoriaService;

    /**
     * Registra un intento exitoso de login.
     * Usa REQUIRES_NEW para que el registro persista aunque la transacción padre falle.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarExito(Usuario usuario) {
        String[] ctx = resolveRequestContext();
        IntentoLogin intento = IntentoLogin.builder()
                .correo(usuario.getCorreo())
                .idUsuario(usuario.getId())
                .idInstitucion(usuario.getIdInstitucion())
                .exito(true)
                .ip(ctx[0])
                .agenteUsuario(ctx[1])
                .build();
        intentoRepo.save(intento);
    }

    /**
     * Registra un intento fallido y dispara alerta si se supera el umbral.
     * Usa REQUIRES_NEW para que el registro persista aunque la transacción padre haga rollback.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarFallo(String correo, String motivoFallo, UUID idUsuario, UUID idInstitucion) {
        String[] ctx = resolveRequestContext();
        IntentoLogin intento = IntentoLogin.builder()
                .correo(correo)
                .idUsuario(idUsuario)
                .idInstitucion(idInstitucion)
                .exito(false)
                .ip(ctx[0])
                .agenteUsuario(ctx[1])
                .motivoFallo(motivoFallo)
                .build();
        intentoRepo.save(intento);

        // Detectar posible ataque de fuerza bruta
        Instant ventana = Instant.now().minus(VENTANA_MINUTOS, ChronoUnit.MINUTES);
        long fallos = intentoRepo.countFallidosDesde(correo, ventana);
        if (fallos >= UMBRAL_FALLOS) {
            log.warn("ALERTA_SEGURIDAD: {} fallos de login en {} min para correo={} ip={}",
                    fallos, VENTANA_MINUTOS, correo, ctx[0]);
            auditoriaService.registrar(idInstitucion, idUsuario,
                    "SEGURIDAD", "ALERTA_SEGURIDAD", "intento_login", null,
                    false,
                    "Correo: " + correo + " — " + fallos + " fallos en " + VENTANA_MINUTOS + " min desde IP " + ctx[0]);
        }
    }

    private String[] resolveRequestContext() {
        String ip = "desconocida";
        String agenteUsuario = null;
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest req = attrs.getRequest();
            String xff = req.getHeader("X-Forwarded-For");
            ip = (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : req.getRemoteAddr();
            agenteUsuario = req.getHeader("User-Agent");
        } catch (IllegalStateException ignored) {
        }
        return new String[]{ip, agenteUsuario};
    }
}
