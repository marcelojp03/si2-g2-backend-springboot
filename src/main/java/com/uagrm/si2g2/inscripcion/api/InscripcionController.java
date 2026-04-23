package com.uagrm.si2g2.inscripcion.api;

import com.uagrm.si2g2.common.dto.ApiResponse;
import com.uagrm.si2g2.inscripcion.application.InscripcionService;
import com.uagrm.si2g2.inscripcion.dto.InscripcionRequest;
import com.uagrm.si2g2.inscripcion.dto.InscripcionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/inscripciones")
@RequiredArgsConstructor
public class InscripcionController {

    private final InscripcionService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO')")
    public ResponseEntity<ApiResponse<InscripcionResponse>> inscribir(
            @Valid @RequestBody InscripcionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Estudiante inscrito", service.inscribir(request)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO')")
    public ResponseEntity<ApiResponse<List<InscripcionResponse>>> listar(
            @RequestParam(required = false) UUID idEstudiante,
            @RequestParam(required = false) UUID idGestion,
            @RequestParam(required = false) UUID idParalelo) {
        return ResponseEntity.ok(ApiResponse.ok("Inscripciones",
                service.listar(idEstudiante, idGestion, idParalelo)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO')")
    public ResponseEntity<ApiResponse<InscripcionResponse>> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Inscripción", service.obtener(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','SECRETARIO')")
    public ResponseEntity<ApiResponse<Void>> anular(@PathVariable UUID id) {
        service.anular(id);
        return ResponseEntity.ok(ApiResponse.ok("Inscripción anulada", null));
    }
}
