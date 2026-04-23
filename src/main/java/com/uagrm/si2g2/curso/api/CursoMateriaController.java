package com.uagrm.si2g2.curso.api;

import com.uagrm.si2g2.common.dto.ApiResponse;
import com.uagrm.si2g2.curso.application.CursoMateriaService;
import com.uagrm.si2g2.curso.dto.CursoMateriaRequest;
import com.uagrm.si2g2.curso.dto.CursoMateriaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cursos/{idCurso}/materias")
@RequiredArgsConstructor
public class CursoMateriaController {

    private final CursoMateriaService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR')")
    public ResponseEntity<ApiResponse<CursoMateriaResponse>> asignar(
            @PathVariable UUID idCurso,
            @Valid @RequestBody CursoMateriaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Materia asignada al curso", service.asignar(idCurso, request)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO','DOCENTE')")
    public ResponseEntity<ApiResponse<List<CursoMateriaResponse>>> listar(@PathVariable UUID idCurso) {
        return ResponseEntity.ok(ApiResponse.ok("Materias del curso", service.listarPorCurso(idCurso)));
    }

    @DeleteMapping("/{idMateria}")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR')")
    public ResponseEntity<ApiResponse<Void>> desasignar(
            @PathVariable UUID idCurso,
            @PathVariable UUID idMateria) {
        service.desasignar(idCurso, idMateria);
        return ResponseEntity.ok(ApiResponse.ok("Materia desasignada del curso", null));
    }
}
