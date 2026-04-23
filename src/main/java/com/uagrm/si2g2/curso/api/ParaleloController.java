package com.uagrm.si2g2.curso.api;

import com.uagrm.si2g2.common.dto.ApiResponse;
import com.uagrm.si2g2.curso.application.ParaleloService;
import com.uagrm.si2g2.curso.dto.ParaleloRequest;
import com.uagrm.si2g2.curso.dto.ParaleloResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/paralelos")
@RequiredArgsConstructor
public class ParaleloController {

    private final ParaleloService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR')")
    public ResponseEntity<ApiResponse<ParaleloResponse>> crear(@Valid @RequestBody ParaleloRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Paralelo creado", service.crear(request)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO','DOCENTE')")
    public ResponseEntity<ApiResponse<List<ParaleloResponse>>> listar(
            @RequestParam(required = false) UUID idCurso) {
        return ResponseEntity.ok(ApiResponse.ok("Paralelos", service.listar(idCurso)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO','DOCENTE')")
    public ResponseEntity<ApiResponse<ParaleloResponse>> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Paralelo", service.obtener(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR')")
    public ResponseEntity<ApiResponse<ParaleloResponse>> actualizar(
            @PathVariable UUID id, @Valid @RequestBody ParaleloRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Paralelo actualizado", service.actualizar(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable UUID id) {
        service.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok("Paralelo desactivado", null));
    }
}
