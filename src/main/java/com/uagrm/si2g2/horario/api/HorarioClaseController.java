package com.uagrm.si2g2.horario.api;

import com.uagrm.si2g2.common.dto.ApiResponse;
import com.uagrm.si2g2.horario.application.HorarioClaseService;
import com.uagrm.si2g2.horario.dto.HorarioClaseRequest;
import com.uagrm.si2g2.horario.dto.HorarioClaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/horarios")
@RequiredArgsConstructor
public class HorarioClaseController {

    private final HorarioClaseService service;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('OPERACION_READ','OPERACION_WRITE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO','DOCENTE')")
    public ResponseEntity<ApiResponse<List<HorarioClaseResponse>>> listar(
            @RequestParam UUID idInstitucion) {
        return ResponseEntity.ok(ApiResponse.ok("Horarios", service.listarActivos(idInstitucion)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('OPERACION_READ','OPERACION_WRITE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO','DOCENTE')")
    public ResponseEntity<ApiResponse<HorarioClaseResponse>> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Horario", service.obtenerPorId(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('OPERACION_WRITE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO')")
    public ResponseEntity<ApiResponse<HorarioClaseResponse>> crear(
            @Valid @RequestBody HorarioClaseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Horario creado", service.crear(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('OPERACION_WRITE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO')")
    public ResponseEntity<ApiResponse<HorarioClaseResponse>> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody HorarioClaseRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Horario actualizado", service.actualizar(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('OPERACION_WRITE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable UUID id) {
        service.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok("Horario desactivado", null));
    }

    @GetMapping("/asignacion/{idAsignacionDocente}")
    @PreAuthorize("hasAnyAuthority('OPERACION_READ','OPERACION_WRITE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO','DOCENTE')")
    public ResponseEntity<ApiResponse<List<HorarioClaseResponse>>> listarPorAsignacionDocente(
            @PathVariable UUID idAsignacionDocente) {
        return ResponseEntity.ok(ApiResponse.ok("Horarios por asignacion",
                service.listarPorAsignacionDocente(idAsignacionDocente)));
    }

    @GetMapping("/aula/{idAula}")
    @PreAuthorize("hasAnyAuthority('OPERACION_READ','OPERACION_WRITE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO','DOCENTE')")
    public ResponseEntity<ApiResponse<List<HorarioClaseResponse>>> listarPorAula(
            @PathVariable UUID idAula) {
        return ResponseEntity.ok(ApiResponse.ok("Horarios por aula", service.listarPorAula(idAula)));
    }
}
