package com.uagrm.si2g2.respaldo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "registro_respaldo", schema = "sia")
public class RegistroRespaldo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "id_institucion")
    private UUID idInstitucion;

    @Column(name = "tipo_respaldo", nullable = false, length = 20)
    private String tipoRespaldo = "POR_TENANT";

    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "PENDIENTE";

    @Column(name = "iniciado_por")
    private UUID iniciadoPor;

    @Column(name = "fecha_inicio")
    private OffsetDateTime fechaInicio = OffsetDateTime.now();

    @Column(name = "fecha_fin")
    private OffsetDateTime fechaFin;

    @Column(name = "ruta_almacenamiento", length = 500)
    private String rutaAlmacenamiento;

    @Column(name = "tamanio_bytes")
    private Long tamanioBytes;

    @Column(name = "observacion", columnDefinition = "TEXT")
    private String observacion;

    @Column(name = "simulado", nullable = false)
    private boolean simulado = false;

    @Column(name = "creado_en", updatable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    @Column(name = "actualizado_en")
    private OffsetDateTime actualizadoEn = OffsetDateTime.now();

    @PreUpdate
    public void onUpdate() {
        this.actualizadoEn = OffsetDateTime.now();
    }
}
