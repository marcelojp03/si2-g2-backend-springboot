package com.uagrm.si2g2.auditoria.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    @Column(name = "metodo_http", length = 10)
    private String metodoHttp;

    @Column(name = "ruta_recurso", length = 255)
    private String rutaRecurso;

    @Column(name = "nombre_funcion", length = 150)
    private String nombreFuncion;

    @Column(name = "nombre_modulo", nullable = false, length = 100)
    private String nombreModulo;

    @Column(name = "nombre_entidad", length = 100)
    private String nombreEntidad;

    @Column(name = "id_entidad", length = 100)
    private String idEntidad;

    @Column(name = "tipo_operacion", nullable = false, length = 30)
    private String tipoOperacion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos_antes", columnDefinition = "jsonb")
    private String datosAntes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos_despues", columnDefinition = "jsonb")
    private String datosDespues;

    @Column(name = "exito", nullable = false)
    @Builder.Default
    private boolean exito = true;

    @Column(name = "mensaje", length = 255)
    private String mensaje;

    @Column(name = "hash_integridad", length = 128)
    private String hashIntegridad;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @PrePersist
    protected void onCreate() {
        if (fechaEvento == null) fechaEvento = Instant.now();
        creadoEn = Instant.now();
    }
}
