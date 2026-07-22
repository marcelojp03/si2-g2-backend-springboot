package com.uagrm.si2g2.estudiante.api;

import com.uagrm.si2g2.common.dto.ApiResponse;
import com.uagrm.si2g2.estudiante.application.EstudianteService;
import com.uagrm.si2g2.estudiante.application.HistorialService;
import com.uagrm.si2g2.estudiante.dto.EstudianteRequest;
import com.uagrm.si2g2.estudiante.dto.EstudianteResponse;
import com.uagrm.si2g2.estudiante.dto.HistorialAcademicoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/estudiantes")
@RequiredArgsConstructor
public class EstudianteController {

    private final EstudianteService service;
    private final HistorialService historialService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO') or hasAuthority('ESTUDIANTES_CREATE')")
    public ResponseEntity<ApiResponse<EstudianteResponse>> crear(@Valid @RequestBody EstudianteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Estudiante registrado", service.crear(request)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO','DOCENTE') or hasAuthority('ESTUDIANTES_READ')")
    public ResponseEntity<ApiResponse<List<EstudianteResponse>>> listar(
            @RequestParam(required = false) UUID idParalelo) {
        return ResponseEntity.ok(ApiResponse.ok("Estudiantes", service.listar(idParalelo)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO') or hasAuthority('ESTUDIANTES_READ')")
    public ResponseEntity<ApiResponse<EstudianteResponse>> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Estudiante", service.obtener(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO') or hasAuthority('ESTUDIANTES_UPDATE')")
    public ResponseEntity<ApiResponse<EstudianteResponse>> actualizar(
            @PathVariable UUID id, @Valid @RequestBody EstudianteRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Estudiante actualizado", service.actualizar(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR') or hasAuthority('ESTUDIANTES_DELETE')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable UUID id) {
        service.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok("Estudiante desactivado", null));
    }

    @GetMapping("/{id}/historial")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO','DOCENTE','ESTUDIANTE','TUTOR')")
    public ResponseEntity<ApiResponse<HistorialAcademicoResponse>> historial(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID idGestion) {
        return ResponseEntity.ok(ApiResponse.ok("Historial académico", historialService.obtener(id, idGestion)));
    }
}
