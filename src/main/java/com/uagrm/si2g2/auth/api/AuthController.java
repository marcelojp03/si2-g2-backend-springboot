package com.uagrm.si2g2.auth.api;

import com.uagrm.si2g2.auth.application.AuthService;
import com.uagrm.si2g2.auth.dto.AuthResponse;
import com.uagrm.si2g2.auth.dto.LoginRequest;
import com.uagrm.si2g2.auth.dto.PasswordRecoveryRequest;
import com.uagrm.si2g2.auth.dto.PasswordRecoveryResponse;
import com.uagrm.si2g2.auth.dto.PasswordRecoveryVerifyRequest;
import com.uagrm.si2g2.auth.dto.PasswordResetRequest;
import com.uagrm.si2g2.auth.dto.RegisterRequest;
import com.uagrm.si2g2.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('USUARIOS_WRITE')")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse data = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Usuario registrado exitosamente", data));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse data = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login exitoso", data));
    }

    @PostMapping("/password-recovery/request")
    public ResponseEntity<ApiResponse<PasswordRecoveryResponse>> requestPasswordRecovery(
            @Valid @RequestBody PasswordRecoveryRequest request) {
        PasswordRecoveryResponse data = authService.requestPasswordRecovery(request);
        return ResponseEntity.ok(ApiResponse.ok(data.getMensaje(), data));
    }

    @PostMapping("/password-recovery/verify")
    public ResponseEntity<ApiResponse<PasswordRecoveryResponse>> verifyPasswordRecovery(
            @Valid @RequestBody PasswordRecoveryVerifyRequest request) {
        PasswordRecoveryResponse data = authService.verifyPasswordRecovery(request);
        return ResponseEntity.ok(ApiResponse.ok(data.getMensaje(), data));
    }

    @PostMapping("/password-recovery/reset")
    public ResponseEntity<ApiResponse<PasswordRecoveryResponse>> resetPassword(
            @Valid @RequestBody PasswordResetRequest request) {
        PasswordRecoveryResponse data = authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok(data.getMensaje(), data));
    }
}
