package com.uagrm.si2g2.saas.pago.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Pago del plan asociado a una solicitud de onboarding (pasarela Vpay).
 *
 * <p>Es PRE-institución: se vincula a {@code solicitud_onboarding}, no a {@code id_institucion},
 * porque la institución sólo se crea al activar la solicitud.</p>
 */
@Entity
@Table(name = "pago_suscripcion")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class PagoSuscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Token público opaco para el link de pago (/pagar/{tokenPago}). */
    @Column(name = "token_pago", unique = true, nullable = false, updatable = false)
    private UUID tokenPago;

    @Column(name = "id_solicitud", nullable = false)
    private UUID idSolicitud;

    @Column(name = "id_plan", nullable = false)
    private UUID idPlan;

    @Column(name = "monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Builder.Default
    @Column(name = "moneda", nullable = false, length = 5)
    private String moneda = "BOB";

    @Builder.Default
    @Column(name = "metodo_pago", nullable = false, length = 30)
    private String metodoPago = "QR";

    @Builder.Default
    @Column(name = "proveedor", nullable = false, length = 50)
    private String proveedor = "VPAY";

    /** Identificador del QR devuelto por Vpay. */
    @Column(name = "referencia_externa", length = 150)
    private String referenciaExterna;

    /** Imagen del QR en base64 (PNG). */
    @Column(name = "qr_base64", columnDefinition = "TEXT")
    private String qrBase64;

    @Builder.Default
    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "PENDIENTE";

    @Column(name = "glosa", length = 150)
    private String glosa;

    @Column(name = "fecha_expiracion")
    private LocalDate fechaExpiracion;

    @Column(name = "pagado_en")
    private Instant pagadoEn;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @PrePersist
    protected void onCreate() {
        if (tokenPago == null) {
            tokenPago = UUID.randomUUID();
        }
        creadoEn = Instant.now();
        actualizadoEn = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        actualizadoEn = Instant.now();
    }
}
