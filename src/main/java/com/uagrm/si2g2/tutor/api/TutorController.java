package com.uagrm.si2g2.tutor.api;

import com.uagrm.si2g2.common.dto.ApiResponse;
import com.uagrm.si2g2.tutor.application.EstudianteTutorService;
import com.uagrm.si2g2.tutor.application.TutorService;
import com.uagrm.si2g2.tutor.dto.EstudianteTutorRequest;
import com.uagrm.si2g2.tutor.dto.EstudianteTutorResponse;
import com.uagrm.si2g2.tutor.dto.TutorRequest;
import com.uagrm.si2g2.tutor.dto.TutorResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tutores")
@RequiredArgsConstructor
public class TutorController {

    private final TutorService tutorService;
    private final EstudianteTutorService estudianteTutorService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO')")
    public ResponseEntity<ApiResponse<TutorResponse>> crear(@Valid @RequestBody TutorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Tutor registrado", tutorService.crear(request)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO')")
    public ResponseEntity<ApiResponse<List<TutorResponse>>> listar() {
        return ResponseEntity.ok(ApiResponse.ok("Tutores", tutorService.listar()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO')")
    public ResponseEntity<ApiResponse<TutorResponse>> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Tutor", tutorService.obtener(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO')")
    public ResponseEntity<ApiResponse<TutorResponse>> actualizar(
            @PathVariable UUID id, @Valid @RequestBody TutorRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Tutor actualizado", tutorService.actualizar(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable UUID id) {
        tutorService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok("Tutor desactivado", null));
    }

    // --- Vínculos estudiante-tutor ---

    @PostMapping("/estudiantes/{idEstudiante}")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','SECRETARIO')")
    public ResponseEntity<ApiResponse<EstudianteTutorResponse>> vincular(
            @PathVariable UUID idEstudiante,
            @Valid @RequestBody EstudianteTutorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Tutor vinculado al estudiante",
                        estudianteTutorService.vincular(idEstudiante, request)));
    }

    @GetMapping("/estudiantes/{idEstudiante}")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO')")
    public ResponseEntity<ApiResponse<List<EstudianteTutorResponse>>> listarPorEstudiante(
            @PathVariable UUID idEstudiante) {
        return ResponseEntity.ok(ApiResponse.ok("Tutores del estudiante",
                estudianteTutorService.listarPorEstudiante(idEstudiante)));
    }

    @DeleteMapping("/estudiantes/{idEstudiante}/{idTutor}")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','SECRETARIO')")
    public ResponseEntity<ApiResponse<Void>> desvincular(
            @PathVariable UUID idEstudiante,
            @PathVariable UUID idTutor) {
        estudianteTutorService.desvincular(idEstudiante, idTutor);
        return ResponseEntity.ok(ApiResponse.ok("Tutor desvinculado del estudiante", null));
    }
}
