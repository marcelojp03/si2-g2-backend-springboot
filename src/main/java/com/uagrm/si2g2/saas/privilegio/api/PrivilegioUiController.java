package com.uagrm.si2g2.saas.privilegio.api;

import com.uagrm.si2g2.common.dto.ApiResponse;
import com.uagrm.si2g2.saas.privilegio.application.PrivilegioUiService;
import com.uagrm.si2g2.saas.privilegio.dto.PrivilegioUiRequest;
import com.uagrm.si2g2.saas.privilegio.dto.PrivilegioUiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/privilegios-ui")
@RequiredArgsConstructor
public class PrivilegioUiController {

    private final PrivilegioUiService service;

    /**
     * Devuelve el mapa de privilegios del usuario autenticado.
     * El frontend llama esto al login y lo almacena en señal reactiva.
     */
    @GetMapping("/mi-mapa")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Map<String, String>>>> obtenerMapaPropio() {
        return ResponseEntity.ok(ApiResponse.ok("Mapa de privilegios UI", service.obtenerMapaUsuarioActual()));
    }

    /**
     * Lista los privilegios configurados para un rol (solo ADMIN_INSTITUCION).
     */
    @GetMapping("/rol/{idRol}")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<PrivilegioUiResponse>>> listarPorRol(@PathVariable UUID idRol) {
        return ResponseEntity.ok(ApiResponse.ok("Privilegios UI del rol", service.listarPorRol(idRol)));
    }

    /**
     * Reemplaza todos los privilegios del rol por los enviados en el cuerpo.
     */
    @PutMapping("/rol/{idRol}")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<PrivilegioUiResponse>>> guardarPrivilegiosRol(
            @PathVariable UUID idRol,
            @Valid @RequestBody List<PrivilegioUiRequest> requests) {
        return ResponseEntity.ok(ApiResponse.ok("Privilegios UI actualizados",
                service.guardarPrivilegiosRol(idRol, requests)));
    }
}
