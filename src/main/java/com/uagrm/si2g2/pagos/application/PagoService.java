package com.uagrm.si2g2.pagos.application;

import com.uagrm.si2g2.auditoria.application.AuditoriaService;
import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.pagos.domain.CuotaEstudiante;
import com.uagrm.si2g2.pagos.domain.CuotaEstudianteRepository;
import com.uagrm.si2g2.pagos.domain.Pago;
import com.uagrm.si2g2.pagos.domain.PagoRepository;
import com.uagrm.si2g2.pagos.dto.PagoResponse;
import com.uagrm.si2g2.pagos.dto.RegistrarPagoRequest;
import com.uagrm.si2g2.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PagoService {

    private final PagoRepository repository;
    private final CuotaEstudianteRepository cuotaRepository;
    private final AuditoriaService auditoriaService;

    @Transactional
    public PagoResponse registrarPago(RegistrarPagoRequest request) {
        UUID idInstitucion = TenantContext.get();
        UUID userId = SecurityUtils.currentUserId();

        CuotaEstudiante cuota = cuotaRepository.findById(request.getIdCuota())
                .orElseThrow(() -> new EntityNotFoundException("Cuota no encontrada"));

        if (!"PENDIENTE".equals(cuota.getEstado())) {
            throw new IllegalStateException("La cuota ya esta " + cuota.getEstado());
        }

        Pago pago = Pago.builder()
                .idInstitucion(idInstitucion)
                .idCuota(cuota.getId())
                .idUsuarioPaga(userId)
                .monto(request.getMonto())
                .metodoPago(request.getMetodoPago() != null ? request.getMetodoPago() : "QR")
                .proveedor(request.getProveedor())
                .referenciaExterna(request.getReferenciaExterna())
                .qrBase64(request.getQrBase64())
                .estado("COMPLETADO")
                .pagadoEn(Instant.now())
                .build();
        PagoResponse resp = PagoResponse.from(repository.save(pago));

        cuota.setEstado("PAGADA");
        cuotaRepository.save(cuota);

        auditoriaService.registrar(idInstitucion, userId,
                "PAGOS", "REGISTRAR_PAGO", "pago", resp.getId().toString(),
                true, "Pago registrado para cuota " + cuota.getId());
        return resp;
    }

    @Transactional(readOnly = true)
    public List<PagoResponse> misPagos(UUID idUsuario) {
        return repository.findByIdUsuarioPagaOrderByCreadoEnDesc(idUsuario)
                .stream().map(PagoResponse::from).toList();
    }
}
