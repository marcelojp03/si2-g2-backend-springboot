package com.uagrm.si2g2.auditoria.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bitacora_auditoria")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BitacoraAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "id_institucion")
    private UUID idInstitucion;

    @Column(name = "id_usuario")
    private UUID idUsuario;

    @Column(name = "fecha_evento", nullable = false)
    private Instant fechaEvento;

    @Column(name = "direccion_ip", length = 50)
    private String direccionIp;

    @Column(name = "plataforma_cliente", length = 30)
    private String plataformaCliente;

    @Column(name = "agente_usuario", columnDefinition = "TEXT")
    private String agenteUsuario;

    @Column(name = "nombre_modulo", nullable = false, length = 100)
    private String nombreModulo;

    @Column(name = "nombre_entidad", length = 100)
    private String nombreEntidad;

    @Column(name = "id_entidad", length = 100)
    private String idEntidad;

    @Column(name = "tipo_operacion", nullable = false, length = 30)
    private String tipoOperacion;

    @Column(name = "exito", nullable = false)
    @Builder.Default
    private boolean exito = true;

    @Column(name = "mensaje", length = 255)
    private String mensaje;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @PrePersist
    protected void onCreate() {
        if (fechaEvento == null) fechaEvento = Instant.now();
        creadoEn = Instant.now();
    }
}
