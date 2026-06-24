package com.uagrm.si2g2.pagos.application;

import com.uagrm.si2g2.auditoria.application.AuditoriaService;
import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.pagos.domain.CuotaEstudiante;
import com.uagrm.si2g2.pagos.domain.CuotaEstudianteRepository;
import com.uagrm.si2g2.pagos.domain.Pago;
import com.uagrm.si2g2.pagos.domain.PagoRepository;
import com.uagrm.si2g2.pagos.dto.PagoResponse;
import com.uagrm.si2g2.pagos.dto.RegistrarPagoRequest;
import com.uagrm.si2g2.saas.pago.application.VpayClient;
import com.uagrm.si2g2.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PagoService {

    private final PagoRepository repository;
    private final CuotaEstudianteRepository cuotaRepository;
    private final AuditoriaService auditoriaService;
    private final VpayClient vpayClient;

    @Transactional
    public PagoResponse generarQrPago(UUID idCuota) {
        UUID idInstitucion = TenantContext.getOrThrow();
        UUID userId = SecurityUtils.currentUserId();

        CuotaEstudiante cuota = cuotaRepository.findById(idCuota)
                .orElseThrow(() -> new EntityNotFoundException("Cuota no encontrada"));

        if (!"PENDIENTE".equals(cuota.getEstado())) {
            throw new IllegalStateException("La cuota ya esta " + cuota.getEstado());
        }

        var qr = vpayClient.generarQr(
                cuota.getMonto(),
                "Cuota #" + cuota.getNumeroCuota() + " - " + cuota.getId(),
                LocalDate.now().plusDays(7),
                idCuota.toString()
        );

        Pago pago = Pago.builder()
                .idInstitucion(idInstitucion)
                .idCuota(cuota.getId())
                .idUsuarioPaga(userId)
                .monto(cuota.getMonto())
                .metodoPago("QR")
                .proveedor(vpayClient.isDemo() ? "VPAY_DEMO" : "VPAY")
                .referenciaExterna(qr.idQr())
                .qrBase64(qr.qrBase64())
                .estado("PENDIENTE")
                .build();
        PagoResponse resp = PagoResponse.from(repository.save(pago));

        auditoriaService.registrar(idInstitucion, userId,
                "PAGOS", "GENERAR_QR_PAGO", "pago", resp.getId().toString(),
                true, "QR generado para cuota " + idCuota);
        return resp;
    }

    @Transactional
    public PagoResponse consultarEstado(UUID idPago) {
        Pago pago = repository.findById(idPago)
                .orElseThrow(() -> new EntityNotFoundException("Pago no encontrado"));

        if ("COMPLETADO".equals(pago.getEstado())) {
            return PagoResponse.from(pago);
        }

        String estadoVpay = vpayClient.consultarEstadoQr(pago.getReferenciaExterna());
        if ("PAG".equals(estadoVpay)) {
            pago.setEstado("COMPLETADO");
            pago.setPagadoEn(Instant.now());
            repository.save(pago);

            CuotaEstudiante cuota = cuotaRepository.findById(pago.getIdCuota())
                    .orElse(null);
            if (cuota != null && "PENDIENTE".equals(cuota.getEstado())) {
                cuota.setEstado("PAGADA");
                cuotaRepository.save(cuota);
            }

            auditoriaService.registrar(pago.getIdInstitucion(), pago.getIdUsuarioPaga(),
                    "PAGOS", "CONFIRMAR_PAGO_VPAY", "pago", pago.getId().toString(),
                    true, "Pago confirmado por Vpay para cuota " + pago.getIdCuota());
        }
        return PagoResponse.from(repository.findById(idPago).orElse(pago));
    }

    @Transactional
    public PagoResponse confirmarPago(UUID idPago) {
        Pago pago = repository.findById(idPago)
                .orElseThrow(() -> new EntityNotFoundException("Pago no encontrado"));

        if ("COMPLETADO".equals(pago.getEstado())) {
            return PagoResponse.from(pago);
        }

        pago.setEstado("COMPLETADO");
        pago.setPagadoEn(Instant.now());
        repository.save(pago);

        CuotaEstudiante cuota = cuotaRepository.findById(pago.getIdCuota())
                .orElse(null);
        if (cuota != null && "PENDIENTE".equals(cuota.getEstado())) {
            cuota.setEstado("PAGADA");
            cuotaRepository.save(cuota);
        }

        auditoriaService.registrar(pago.getIdInstitucion(), SecurityUtils.currentUserId(),
                "PAGOS", "CONFIRMAR_PAGO_MANUAL", "pago", pago.getId().toString(),
                true, "Pago confirmado manualmente para cuota " + pago.getIdCuota());
        return PagoResponse.from(pago);
    }

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
