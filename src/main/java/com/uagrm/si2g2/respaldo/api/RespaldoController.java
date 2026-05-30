package com.uagrm.si2g2.respaldo.api;

import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.common.dto.ApiResponse;
import com.uagrm.si2g2.respaldo.application.RespaldoService;
import com.uagrm.si2g2.respaldo.domain.RegistroRespaldo;
import com.uagrm.si2g2.respaldo.domain.RegistroRestauracion;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RespaldoController {

    private final RespaldoService service;

    // -------------------------------------------------------------------------
    // RESPALDOS
    // -------------------------------------------------------------------------

    @GetMapping("/respaldos")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_INSTITUCION')")
    public ResponseEntity<ApiResponse<List<RegistroRespaldo>>> listarRespaldos() {
        UUID idInstitucion = null;
        // SUPER_ADMIN ve todos; ADMIN_INSTITUCION ve solo los suyos
        if (!SecurityUtils.currentUserHasRole("SUPER_ADMIN")) {
            idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        }
        return ResponseEntity.ok(ApiResponse.ok("Respaldos", service.listarRespaldos(idInstitucion)));
    }

    @PostMapping("/respaldos")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_INSTITUCION')")
    public ResponseEntity<ApiResponse<RegistroRespaldo>> iniciarRespaldo() {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        return ResponseEntity.ok(ApiResponse.ok("Respaldo iniciado", service.iniciarRespaldo(idInstitucion)));
    }

    // -------------------------------------------------------------------------
    // RESTAURACIONES
    // -------------------------------------------------------------------------

    @GetMapping("/restauraciones")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_INSTITUCION')")
    public ResponseEntity<ApiResponse<List<RegistroRestauracion>>> listarRestauraciones() {
        UUID idInstitucion = null;
        if (!SecurityUtils.currentUserHasRole("SUPER_ADMIN")) {
            idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        }
        return ResponseEntity.ok(ApiResponse.ok("Solicitudes de restauración", service.listarRestauraciones(idInstitucion)));
    }

    @PostMapping("/restauraciones")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_INSTITUCION')")
    public ResponseEntity<ApiResponse<RegistroRestauracion>> solicitarRestauracion(
            @RequestBody Map<String, String> body) {
        UUID idRespaldo = UUID.fromString(body.get("idRespaldo"));
        String motivo = body.get("motivo");
        return ResponseEntity.ok(ApiResponse.ok("Solicitud de restauración creada",
                service.solicitarRestauracion(idRespaldo, motivo)));
    }

    @PutMapping("/restauraciones/{id}/aprobar")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<RegistroRestauracion>> aprobarRestauracion(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Restauración aprobada", service.aprobarRestauracion(id)));
    }
}
