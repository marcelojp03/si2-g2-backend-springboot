package com.uagrm.si2g2.aula.api;

import com.uagrm.si2g2.aula.application.AulaService;
import com.uagrm.si2g2.aula.dto.AulaRequest;
import com.uagrm.si2g2.aula.dto.AulaResponse;
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
@RequestMapping("/api/aulas")
@RequiredArgsConstructor
public class AulaController {

    private final AulaService service;

    @PostMapping
    @PreAuthorize("hasAuthority('AULAS_CREATE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR')")
    public ResponseEntity<ApiResponse<AulaResponse>> crear(@Valid @RequestBody AulaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Aula creada", service.crear(request)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('AULAS_READ') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO','DOCENTE')")
    public ResponseEntity<ApiResponse<List<AulaResponse>>> listar(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Integer capacidadMin,
            @RequestParam(required = false) Integer capacidadMax,
            @RequestParam(required = false) String recurso,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(ApiResponse.ok("Aulas", service.listar(estado, capacidadMin, capacidadMax, recurso, q)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('AULAS_READ') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO','DOCENTE')")
    public ResponseEntity<ApiResponse<AulaResponse>> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Aula", service.obtener(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('AULAS_UPDATE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR')")
    public ResponseEntity<ApiResponse<AulaResponse>> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody AulaRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Aula actualizada", service.actualizar(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('AULAS_DELETE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable UUID id) {
        service.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok("Aula desactivada", null));
    }
}
