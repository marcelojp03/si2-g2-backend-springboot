package com.uagrm.si2g2.auth.api;

import com.uagrm.si2g2.auth.application.RoleService;
import com.uagrm.si2g2.auth.dto.PermisoResponse;
import com.uagrm.si2g2.auth.dto.RolRequest;
import com.uagrm.si2g2.auth.dto.RolResponse;
import com.uagrm.si2g2.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ROLES_READ') or hasAuthority('ROLES_WRITE')")
    public ResponseEntity<ApiResponse<List<RolResponse>>> listarRoles() {
        return ResponseEntity.ok(ApiResponse.ok("Roles", roleService.listarRolesDisponibles()));
    }

    @GetMapping("/asignables")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('USUARIOS_WRITE') or hasAuthority('ROLES_READ') or hasAuthority('ROLES_WRITE')")
    public ResponseEntity<ApiResponse<List<RolResponse>>> listarAsignables() {
        return ResponseEntity.ok(ApiResponse.ok("Roles asignables", roleService.listarRolesAsignables()));
    }

    @GetMapping("/permisos")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ROLES_READ') or hasAuthority('ROLES_WRITE')")
    public ResponseEntity<ApiResponse<List<PermisoResponse>>> listarPermisos() {
        return ResponseEntity.ok(ApiResponse.ok("Permisos", roleService.listarPermisos()));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ROLES_WRITE')")
    public ResponseEntity<ApiResponse<RolResponse>> crear(@Valid @RequestBody RolRequest request) {
        return ResponseEntity.status(201)
                .body(ApiResponse.created("Rol institucional creado", roleService.crearRolInstitucional(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ROLES_WRITE')")
    public ResponseEntity<ApiResponse<RolResponse>> actualizar(@PathVariable UUID id, @Valid @RequestBody RolRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Rol institucional actualizado", roleService.actualizarRolInstitucional(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ROLES_WRITE')")
    public ResponseEntity<ApiResponse<Void>> desactivar(@PathVariable UUID id) {
        roleService.desactivarRolInstitucional(id);
        return ResponseEntity.ok(ApiResponse.ok("Rol institucional desactivado", null));
    }
}
