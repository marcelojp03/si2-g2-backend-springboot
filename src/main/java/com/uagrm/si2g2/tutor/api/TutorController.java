package com.uagrm.si2g2.tutor.api;

import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.common.dto.ApiResponse;
import com.uagrm.si2g2.estudiante.application.EstudianteService;
import com.uagrm.si2g2.estudiante.dto.EstudianteResponse;
import com.uagrm.si2g2.tutor.application.EstudianteTutorService;
import com.uagrm.si2g2.tutor.application.TutorService;
import com.uagrm.si2g2.tutor.domain.EstudianteTutorRepository;
import com.uagrm.si2g2.tutor.domain.Tutor;
import com.uagrm.si2g2.tutor.domain.TutorRepository;
import com.uagrm.si2g2.tutor.dto.EstudianteTutorRequest;
import com.uagrm.si2g2.tutor.dto.EstudianteTutorResponse;
import com.uagrm.si2g2.tutor.dto.TutorRequest;
import com.uagrm.si2g2.tutor.dto.TutorResponse;
import jakarta.persistence.EntityNotFoundException;
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
    private final TutorRepository tutorRepository;
    private final EstudianteTutorRepository estudianteTutorRepository;
    private final EstudianteService estudianteService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO') or hasAuthority('TUTORES_CREATE')")
    public ResponseEntity<ApiResponse<TutorResponse>> crear(@Valid @RequestBody TutorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Tutor registrado", tutorService.crear(request)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO') or hasAuthority('TUTORES_READ')")
    public ResponseEntity<ApiResponse<List<TutorResponse>>> listar() {
        return ResponseEntity.ok(ApiResponse.ok("Tutores", tutorService.listar()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO') or hasAuthority('TUTORES_READ')")
    public ResponseEntity<ApiResponse<TutorResponse>> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Tutor", tutorService.obtener(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO') or hasAuthority('TUTORES_UPDATE')")
    public ResponseEntity<ApiResponse<TutorResponse>> actualizar(
            @PathVariable UUID id, @Valid @RequestBody TutorRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Tutor actualizado", tutorService.actualizar(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR') or hasAuthority('TUTORES_DELETE')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable UUID id) {
        tutorService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok("Tutor desactivado", null));
    }

    @GetMapping("/mi-estudiante")
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<ApiResponse<EstudianteResponse>> miEstudiante() {
        UUID userId = SecurityUtils.currentUserId();
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        Tutor tutor = tutorRepository.findByIdUsuarioAndIdInstitucion(userId, idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Tutor no encontrado para el usuario actual"));
        var vinculos = estudianteTutorRepository.findAllByIdInstitucionAndIdTutor(idInstitucion, tutor.getId());
        if (vinculos.isEmpty()) {
            throw new EntityNotFoundException("No hay estudiantes vinculados a este tutor");
        }
        return ResponseEntity.ok(ApiResponse.ok("Estudiante vinculado",
                estudianteService.obtener(vinculos.getFirst().getIdEstudiante())));
    }

    // --- Vínculos estudiante-tutor ---

    @PostMapping("/estudiantes/{idEstudiante}")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','SECRETARIO') or hasAuthority('TUTORES_UPDATE')")
    public ResponseEntity<ApiResponse<EstudianteTutorResponse>> vincular(
            @PathVariable UUID idEstudiante,
            @Valid @RequestBody EstudianteTutorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Tutor vinculado al estudiante",
                        estudianteTutorService.vincular(idEstudiante, request)));
    }

    @GetMapping("/estudiantes/{idEstudiante}")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO') or hasAuthority('TUTORES_READ')")
    public ResponseEntity<ApiResponse<List<EstudianteTutorResponse>>> listarPorEstudiante(
            @PathVariable UUID idEstudiante) {
        return ResponseEntity.ok(ApiResponse.ok("Tutores del estudiante",
                estudianteTutorService.listarPorEstudiante(idEstudiante)));
    }

    @DeleteMapping("/estudiantes/{idEstudiante}/{idTutor}")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','SECRETARIO') or hasAuthority('TUTORES_UPDATE')")
    public ResponseEntity<ApiResponse<Void>> desvincular(
            @PathVariable UUID idEstudiante,
            @PathVariable UUID idTutor) {
        estudianteTutorService.desvincular(idEstudiante, idTutor);
        return ResponseEntity.ok(ApiResponse.ok("Tutor desvinculado del estudiante", null));
    }
}
