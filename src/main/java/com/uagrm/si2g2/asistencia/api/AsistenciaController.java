package com.uagrm.si2g2.asistencia.api;

import com.uagrm.si2g2.asistencia.application.AsistenciaService;
import com.uagrm.si2g2.asistencia.dto.AsistenciaAsignacionResponse;
import com.uagrm.si2g2.asistencia.dto.AsistenciaPlantillaResponse;
import com.uagrm.si2g2.asistencia.dto.AsistenciaRegistroRequest;
import com.uagrm.si2g2.asistencia.dto.AsistenciaRegistroResponse;
import com.uagrm.si2g2.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/asistencias")
@RequiredArgsConstructor
public class AsistenciaController {

    private final AsistenciaService service;

    @GetMapping("/mis-asignaciones")
    @PreAuthorize("hasAnyAuthority('ASISTENCIA_READ','ASISTENCIA_WRITE','ASISTENCIA_READ_ALL','MI_AREA_READ') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE')")
    public ResponseEntity<ApiResponse<List<AsistenciaAsignacionResponse>>> listarMisAsignaciones() {
        return ResponseEntity.ok(ApiResponse.ok("Asignaciones disponibles", service.listarMisAsignaciones()));
    }

    @GetMapping("/plantilla")
    @PreAuthorize("hasAnyAuthority('ASISTENCIA_READ','ASISTENCIA_WRITE','ASISTENCIA_READ_ALL','MI_AREA_READ') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE')")
    public ResponseEntity<ApiResponse<AsistenciaPlantillaResponse>> obtenerPlantilla(
            @RequestParam UUID idAsignacionDocente,
            @RequestParam LocalDate fecha) {
        return ResponseEntity.ok(ApiResponse.ok("Plantilla de asistencia", service.obtenerPlantilla(idAsignacionDocente, fecha)));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ASISTENCIA_WRITE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE')")
    public ResponseEntity<ApiResponse<AsistenciaRegistroResponse>> guardar(
            @Valid @RequestBody AsistenciaRegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Asistencia guardada", service.guardar(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ASISTENCIA_READ','ASISTENCIA_WRITE','ASISTENCIA_READ_ALL','MI_AREA_READ') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE')")
    public ResponseEntity<ApiResponse<AsistenciaRegistroResponse>> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Asistencia", service.obtener(id)));
    }
}