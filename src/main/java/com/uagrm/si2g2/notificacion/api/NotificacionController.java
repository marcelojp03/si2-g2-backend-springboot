package com.uagrm.si2g2.notificacion.api;

import com.uagrm.si2g2.auth.domain.Usuario;
import com.uagrm.si2g2.auth.domain.UsuarioRepository;
import com.uagrm.si2g2.common.dto.ApiResponse;
import com.uagrm.si2g2.notificacion.application.FcmService;
import com.uagrm.si2g2.notificacion.dto.FcmTokenRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints para gestión de notificaciones push (FCM).
 */
@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final UsuarioRepository usuarioRepository;
    private final FcmService fcmService;

    /**
     * Registra o actualiza el FCM token del usuario autenticado.
     * Llamado desde Flutter al iniciar sesión o cuando FCM rota el token.
     *
     * POST /api/notificaciones/fcm-token
     */
    @PostMapping("/fcm-token")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> registrarFcmToken(
            @AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody FcmTokenRequest request) {

        usuario.setFcmToken(request.fcmToken());
        usuarioRepository.save(usuario);

        return ResponseEntity.ok(ApiResponse.ok("FCM token registrado correctamente", null));
    }

    /**
     * Envía una notificación de prueba al dispositivo del usuario autenticado.
     * Solo para ADMIN_INSTITUCION y SUPER_ADMIN.
     *
     * POST /api/notificaciones/test
     */
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
                "Notificación de prueba",
                "Firebase Cloud Messaging configurado correctamente",
                null);

        return ResponseEntity.ok(ApiResponse.ok("Notificación de prueba enviada", null));
    }
}
