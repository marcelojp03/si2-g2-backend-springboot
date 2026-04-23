package com.uagrm.si2g2.docente.api;

import com.uagrm.si2g2.common.dto.ApiResponse;
import com.uagrm.si2g2.docente.application.DocenteService;
import com.uagrm.si2g2.docente.dto.DocenteRequest;
import com.uagrm.si2g2.docente.dto.DocenteResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/docentes")
@RequiredArgsConstructor
public class DocenteController {

    private final DocenteService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR')")
    public ResponseEntity<ApiResponse<DocenteResponse>> crear(@Valid @RequestBody DocenteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Docente registrado", service.crear(request)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO')")
    public ResponseEntity<ApiResponse<List<DocenteResponse>>> listar() {
        return ResponseEntity.ok(ApiResponse.ok("Docentes", service.listar()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO')")
    public ResponseEntity<ApiResponse<DocenteResponse>> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Docente", service.obtener(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR')")
    public ResponseEntity<ApiResponse<DocenteResponse>> actualizar(
            @PathVariable UUID id, @Valid @RequestBody DocenteRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Docente actualizado", service.actualizar(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable UUID id) {
        service.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok("Docente desactivado", null));
    }
}
