package com.uagrm.si2g2.pagos.api;

import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.common.dto.ApiResponse;
import com.uagrm.si2g2.estudiante.domain.EstudianteRepository;
import com.uagrm.si2g2.pagos.application.CuotaService;
import com.uagrm.si2g2.pagos.application.PagoService;
import com.uagrm.si2g2.pagos.dto.CuotaEstudianteResponse;
import com.uagrm.si2g2.pagos.dto.GenerarCuotasRequest;
import com.uagrm.si2g2.pagos.dto.PagoResponse;
import com.uagrm.si2g2.pagos.dto.RegistrarPagoRequest;
import com.uagrm.si2g2.tutor.domain.EstudianteTutorRepository;
import com.uagrm.si2g2.tutor.domain.TutorRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.uagrm.si2g2.pagos.dto.EstadoPagoResponse;
import com.uagrm.si2g2.pagos.dto.GenerarQrResponse;

@RestController
@RequestMapping("/api/cuotas")
@RequiredArgsConstructor
public class CuotaController {

    private final CuotaService cuotaService;
    private final PagoService pagoService;
    private final EstudianteRepository estudianteRepository;
    private final TutorRepository tutorRepository;
    private final EstudianteTutorRepository estudianteTutorRepository;

    @PostMapping("/generar")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> generar(
            @Valid @RequestBody GenerarCuotasRequest request) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        int total = cuotaService.generarCuotas(idInstitucion, request.getIdPlanPago(),
                request.getIdGestionAcademica(), request.getIdEstudiante());
        return ResponseEntity.ok(ApiResponse.ok("Cuotas generadas", Map.of("creadas", total)));
    }

    @GetMapping("/mis-cuotas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<CuotaEstudianteResponse>>> misCuotas(
            @RequestParam(required = false) UUID idGestion) {
        var user = SecurityUtils.currentUser();
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.ok("Mis cuotas", List.of()));
        }
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();

        // Buscar idEstudiante: primero como estudiante, luego como tutor
        var estOpt = estudianteRepository.findByIdUsuarioAndIdInstitucion(user.getId(), idInstitucion);
        UUID idEstudiante = estOpt.map(e -> (UUID) e.getId()).orElse(null);
        if (idEstudiante == null) {
            // Es tutor? buscar estudiante vinculado
            var tutor = tutorRepository.findByIdUsuarioAndIdInstitucion(user.getId(), idInstitucion)
                    .orElseThrow(() -> new EntityNotFoundException("No hay estudiante vinculado a este usuario"));
            var vinculos = estudianteTutorRepository.findAllByIdInstitucionAndIdTutor(idInstitucion, tutor.getId());
            if (vinculos.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.ok("Mis cuotas", List.of()));
            }
            idEstudiante = vinculos.getFirst().getIdEstudiante();
        }

        if (idGestion == null) {
            return ResponseEntity.ok(ApiResponse.ok("Mis cuotas", cuotaService.listarPorEstudiante(idEstudiante, null)));
        }
        return ResponseEntity.ok(ApiResponse.ok("Mis cuotas", cuotaService.listarPorEstudiante(idEstudiante, idGestion)));
    }

    @PostMapping("/pagar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PagoResponse>> pagar(
            @Valid @RequestBody RegistrarPagoRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Pago registrado", pagoService.registrarPago(request)));
    }

    @GetMapping("/mis-pagos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<PagoResponse>>> misPagos() {
        return ResponseEntity.ok(ApiResponse.ok("Mis pagos",
                pagoService.misPagos(SecurityUtils.currentUserId())));
    }

    @PostMapping("/{idCuota}/generar-qr")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<GenerarQrResponse>> generarQr(
            @PathVariable UUID idCuota) {
        PagoResponse pago = pagoService.generarQrPago(idCuota);
        GenerarQrResponse resp = GenerarQrResponse.builder()
                .idPago(pago.getId())
                .qrBase64(pago.getQrBase64())
                .proveedor(pago.getProveedor())
                .referenciaExterna(pago.getReferenciaExterna())
                .build();
        return ResponseEntity.ok(ApiResponse.ok("QR generado", resp));
    }

    @GetMapping("/pago/{idPago}/estado")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<EstadoPagoResponse>> estadoPago(
            @PathVariable UUID idPago) {
        PagoResponse pago = pagoService.consultarEstado(idPago);
        boolean pagado = "COMPLETADO".equals(pago.getEstado());
        EstadoPagoResponse resp = new EstadoPagoResponse(
                pago.getId(), pago.getReferenciaExterna(),
                pagado ? "PAG" : "PEN", pagado, pago.getEstado());
        return ResponseEntity.ok(ApiResponse.ok("Estado del pago", resp));
    }

    @PostMapping("/pago/{idPago}/confirmar")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PagoResponse>> confirmarPago(
            @PathVariable UUID idPago) {
        return ResponseEntity.ok(ApiResponse.ok("Pago confirmado",
                pagoService.confirmarPago(idPago)));
    }
}
