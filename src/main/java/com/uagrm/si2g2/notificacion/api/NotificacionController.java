package com.uagrm.si2g2.notificacion.api;

import com.uagrm.si2g2.auth.domain.Usuario;
import com.uagrm.si2g2.auth.domain.UsuarioRepository;
import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.common.dto.ApiResponse;
import com.uagrm.si2g2.notificacion.application.FcmService;
import com.uagrm.si2g2.notificacion.application.NotificacionService;
import com.uagrm.si2g2.notificacion.dto.FcmTokenRequest;
import com.uagrm.si2g2.notificacion.dto.NotificacionResponse;
import com.uagrm.si2g2.notificacion.dto.RegistrarDispositivoRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final UsuarioRepository usuarioRepository;
    private final FcmService fcmService;
    private final NotificacionService notificacionService;

    @GetMapping("/mis-notificaciones")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<NotificacionResponse>>> misNotificaciones(
            @RequestParam(defaultValue = "false") boolean soloNoLeidas,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        UUID userId = SecurityUtils.currentUserId();
        if (soloNoLeidas) {
            return ResponseEntity.ok(ApiResponse.ok("Notificaciones no leidas", notificacionService.listarNoLeidas(userId)));
        }
        return ResponseEntity.ok(ApiResponse.ok("Notificaciones", notificacionService.listar(userId, page, size)));
    }

    @GetMapping("/contar-no-leidas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Long>>> contarNoLeidas() {
        long count = notificacionService.contarNoLeidas(SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.ok("Ok", Map.of("total", count)));
    }

    @PutMapping("/{id}/leer")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> marcarLeida(@PathVariable UUID id) {
        notificacionService.marcarLeida(id);
        return ResponseEntity.ok(ApiResponse.ok("Marcada como leida", null));
    }

    @PutMapping("/leer-todas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> marcarTodasLeidas() {
        notificacionService.marcarTodasLeidas(SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.ok("Todas marcadas como leidas", null));
    }

    @PostMapping("/registrar-dispositivo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> registrarDispositivo(
            @AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody RegistrarDispositivoRequest request) {
        notificacionService.registrarDispositivo(
                SecurityUtils.requireCurrentInstitutionId(), usuario.getId(),
                request.tokenDispositivo(), request.plataforma());
        return ResponseEntity.ok(ApiResponse.ok("Dispositivo registrado", null));
    }

    @PostMapping("/fcm-token")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> registrarFcmToken(
            @AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody FcmTokenRequest request) {
        usuario.setFcmToken(request.fcmToken());
        usuarioRepository.save(usuario);
        return ResponseEntity.ok(ApiResponse.ok("FCM token registrado", null));
    }

    @PostMapping("/test")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_INSTITUCION')")
    public ResponseEntity<ApiResponse<Void>> enviarPrueba(
            @AuthenticationPrincipal Usuario usuario) {
        if (usuario.getFcmToken() == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "El usuario no tiene FCM token registrado"));
        }
        fcmService.enviarADispositivo(
                usuario.getFcmToken(),
                "Notificacion de prueba",
                "Firebase Cloud Messaging configurado correctamente",
                null);
        return ResponseEntity.ok(ApiResponse.ok("Notificacion de prueba enviada", null));
    }
}
