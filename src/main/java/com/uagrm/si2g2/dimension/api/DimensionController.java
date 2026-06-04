package com.uagrm.si2g2.dimension.api;

import com.uagrm.si2g2.common.dto.ApiResponse;
import com.uagrm.si2g2.dimension.application.DimensionService;
import com.uagrm.si2g2.dimension.dto.DimensionRequest;
import com.uagrm.si2g2.dimension.dto.DimensionResponse;
import com.uagrm.si2g2.dimension.dto.PeriodoDimensionRequest;
import com.uagrm.si2g2.dimension.dto.PeriodoDimensionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/dimensiones")
@RequiredArgsConstructor
public class DimensionController {

    private final DimensionService service;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('GESTIONES_READ','CALIFICACIONES_READ') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO')")
    public ApiResponse<List<DimensionResponse>> listar() {
        return ApiResponse.ok("Dimensiones disponibles", service.listarDisponibles());
    }

    @GetMapping("/globales")
    @PreAuthorize("hasAuthority('GESTIONES_READ') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR')")
    public ApiResponse<List<DimensionResponse>> listarGlobales() {
        return ApiResponse.ok("Dimensiones globales", service.listarGlobales());
    }

    @PostMapping("/institucion")
    @PreAuthorize("hasAuthority('GESTIONES_CREATE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DimensionResponse> crearInstitucional(@Valid @RequestBody DimensionRequest request) {
        return ApiResponse.created("Dimensión creada", service.crearInstitucional(request));
    }

    @PostMapping("/global")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DimensionResponse> crearGlobal(@Valid @RequestBody DimensionRequest request) {
        return ApiResponse.created("Dimensión global creada", service.crearGlobal(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('GESTIONES_UPDATE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR')")
    public ApiResponse<DimensionResponse> actualizar(@PathVariable UUID id, @Valid @RequestBody DimensionRequest request) {
        return ApiResponse.ok("Dimensión actualizada", service.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('GESTIONES_DELETE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR')")
    public ApiResponse<Void> eliminar(@PathVariable UUID id) {
        service.eliminar(id);
        return ApiResponse.ok("Dimensión desactivada", null);
    }

    @GetMapping("/periodos/{idPeriodo}")
    @PreAuthorize("hasAnyAuthority('GESTIONES_READ','CALIFICACIONES_READ') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO')")
    public ApiResponse<List<PeriodoDimensionResponse>> pesosPeriodo(@PathVariable UUID idPeriodo) {
        return ApiResponse.ok("Pesos del período", service.listarPesosPeriodo(idPeriodo));
    }

    @PutMapping("/periodos/{idPeriodo}")
    @PreAuthorize("hasAuthority('GESTIONES_UPDATE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR')")
    public ApiResponse<List<PeriodoDimensionResponse>> actualizarPesosPeriodo(
            @PathVariable UUID idPeriodo,
            @Valid @RequestBody List<PeriodoDimensionRequest> solicitudes) {
        return ApiResponse.ok("Pesos actualizados", service.actualizarPesosPeriodo(idPeriodo, solicitudes));
    }
}
