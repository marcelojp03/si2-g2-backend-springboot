package com.uagrm.si2g2.institucion.api;

import com.uagrm.si2g2.common.dto.ApiResponse;
import com.uagrm.si2g2.institucion.application.InstitucionService;
import com.uagrm.si2g2.institucion.dto.ConfiguracionInstitucionRequest;
import com.uagrm.si2g2.institucion.dto.ConfiguracionInstitucionResponse;
import com.uagrm.si2g2.institucion.dto.InstitucionRequest;
import com.uagrm.si2g2.institucion.dto.InstitucionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/instituciones")
@RequiredArgsConstructor
public class InstitucionController {

    private final InstitucionService service;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<InstitucionResponse>> crear(@Valid @RequestBody InstitucionRequest request) {
        return ResponseEntity.status(201)
                .body(ApiResponse.created("Institución creada", service.crear(request)));
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<InstitucionResponse>>> listar() {
        return ResponseEntity.ok(ApiResponse.ok("Instituciones", service.listar()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_INSTITUCION','DIRECTOR','SECRETARIO')")
    public ResponseEntity<ApiResponse<InstitucionResponse>> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Institución", service.obtener(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_INSTITUCION')")
    public ResponseEntity<ApiResponse<InstitucionResponse>> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody InstitucionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Institución actualizada", service.actualizar(id, request)));
    }

    // --- Configuración ---

    @GetMapping("/{id}/configuraciones")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_INSTITUCION','DIRECTOR')")
    public ResponseEntity<ApiResponse<List<ConfiguracionInstitucionResponse>>> listarConfiguraciones(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Configuraciones", service.listarConfiguraciones(id)));
    }

    @PutMapping("/{id}/configuraciones")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_INSTITUCION')")
    public ResponseEntity<ApiResponse<ConfiguracionInstitucionResponse>> guardarConfiguracion(
            @PathVariable UUID id,
            @Valid @RequestBody ConfiguracionInstitucionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Configuración guardada",
                service.guardarConfiguracion(id, request)));
    }

    @DeleteMapping("/{id}/configuraciones/{clave}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_INSTITUCION','DIRECTOR','SECRETARIO')")
    public ResponseEntity<ApiResponse<Void>> eliminarConfiguracion(
            @PathVariable UUID id,
            @PathVariable String clave) {
        service.eliminarConfiguracion(id, clave);
        return ResponseEntity.ok(ApiResponse.ok("Configuración eliminada", null));
    }
}
