package com.uagrm.si2g2.pagos.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "pago")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "id_institucion", nullable = false)
    private UUID idInstitucion;

    @Column(name = "id_cuota", nullable = false)
    private UUID idCuota;

    @Column(name = "id_usuario_paga", nullable = false)
    private UUID idUsuarioPaga;

    @Column(name = "monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Builder.Default
    @Column(name = "moneda", nullable = false, length = 3)
    private String moneda = "BOB";

    @Builder.Default
    @Column(name = "metodo_pago", nullable = false, length = 30)
    private String metodoPago = "QR";

    @Column(name = "proveedor", length = 30)
    private String proveedor;

    @Column(name = "referencia_externa", length = 200)
    private String referenciaExterna;

    @Column(name = "token_pago")
    private UUID tokenPago;

    @Column(name = "qr_base64", columnDefinition = "TEXT")
    private String qrBase64;

    @Builder.Default
    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "PENDIENTE";

    @Column(name = "pagado_en")
    private Instant pagadoEn;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        creadoEn = now;
        actualizadoEn = now;
    }

    @PreUpdate
    protected void onUpdate() {
        actualizadoEn = Instant.now();
    }
}
