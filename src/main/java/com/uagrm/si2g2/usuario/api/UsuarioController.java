package com.uagrm.si2g2.usuario.api;

import com.uagrm.si2g2.common.dto.ApiResponse;
import com.uagrm.si2g2.usuario.application.UsuarioService;
import com.uagrm.si2g2.usuario.dto.ActualizarUsuarioRequest;
import com.uagrm.si2g2.usuario.dto.AsignarRolRequest;
import com.uagrm.si2g2.usuario.dto.UsuarioResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_INSTITUCION','DIRECTOR')")
    public ResponseEntity<ApiResponse<List<UsuarioResponse>>> listar() {
        return ResponseEntity.ok(ApiResponse.ok("Usuarios", service.listar()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_INSTITUCION','DIRECTOR')")
    public ResponseEntity<ApiResponse<UsuarioResponse>> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Usuario", service.obtener(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_INSTITUCION')")
    public ResponseEntity<ApiResponse<UsuarioResponse>> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ActualizarUsuarioRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Usuario actualizado", service.actualizar(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_INSTITUCION')")
    public ResponseEntity<ApiResponse<UsuarioResponse>> desactivar(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Usuario desactivado", service.desactivar(id)));
    }

    @PostMapping("/{id}/roles")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_INSTITUCION')")
    public ResponseEntity<ApiResponse<UsuarioResponse>> asignarRol(
            @PathVariable UUID id,
            @Valid @RequestBody AsignarRolRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Rol asignado", service.asignarRol(id, request)));
    }
}
