package com.uagrm.si2g2.asignacion.api;

import com.uagrm.si2g2.asignacion.application.AsignacionDocenteService;
import com.uagrm.si2g2.asignacion.dto.AsignacionDocenteRequest;
import com.uagrm.si2g2.asignacion.dto.AsignacionDocenteResponse;
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
@RequestMapping("/api/asignaciones")
@RequiredArgsConstructor
public class AsignacionDocenteController {

    private final AsignacionDocenteService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR')")
    public ResponseEntity<ApiResponse<AsignacionDocenteResponse>> asignar(
            @Valid @RequestBody AsignacionDocenteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Docente asignado", service.asignar(request)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO')")
    public ResponseEntity<ApiResponse<List<AsignacionDocenteResponse>>> listar(
            @RequestParam(required = false) UUID idDocente,
            @RequestParam(required = false) UUID idGestion,
            @RequestParam(required = false) UUID idParalelo) {
        return ResponseEntity.ok(ApiResponse.ok("Asignaciones",
                service.listar(idDocente, idGestion, idParalelo)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO')")
    public ResponseEntity<ApiResponse<AsignacionDocenteResponse>> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Asignación", service.obtener(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable UUID id) {
        service.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok("Asignación eliminada", null));
    }
}
