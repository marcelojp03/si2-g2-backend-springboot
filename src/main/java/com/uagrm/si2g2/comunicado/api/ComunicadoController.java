package com.uagrm.si2g2.comunicado.api;

import com.uagrm.si2g2.comunicado.application.ComunicadoService;
import com.uagrm.si2g2.comunicado.dto.ComunicadoRequest;
import com.uagrm.si2g2.comunicado.dto.ComunicadoResponse;
import com.uagrm.si2g2.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/comunicados")
@RequiredArgsConstructor
public class ComunicadoController {

    private final ComunicadoService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','DIRECTOR') or hasAuthority('COMUNICADOS_CREATE')")
    public ResponseEntity<ApiResponse<ComunicadoResponse>> crear(@Valid @RequestBody ComunicadoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Comunicado creado", service.crear(request)));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ComunicadoResponse>>> listar(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String tipo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.ok("Comunicados", service.listar(estado, tipo, page, size)));
    }

    @GetMapping("/publicados")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ComunicadoResponse>>> listarPublicados(
            @RequestParam(required = false) String tipo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.ok("Comunicados publicados", service.listarPublicados(tipo, page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ComunicadoResponse>> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Comunicado", service.obtener(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','DIRECTOR') or hasAuthority('COMUNICADOS_UPDATE')")
    public ResponseEntity<ApiResponse<ComunicadoResponse>> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ComunicadoRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Comunicado actualizado", service.actualizar(id, request)));
    }

    @PostMapping("/{id}/publicar")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','DIRECTOR') or hasAuthority('COMUNICADOS_PUBLISH')")
    public ResponseEntity<ApiResponse<ComunicadoResponse>> publicar(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Comunicado publicado", service.publicar(id)));
    }

    @PostMapping("/{id}/archivar")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','DIRECTOR') or hasAuthority('COMUNICADOS_ARCHIVE')")
    public ResponseEntity<ApiResponse<ComunicadoResponse>> archivar(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Comunicado archivado", service.archivar(id)));
    }
}
