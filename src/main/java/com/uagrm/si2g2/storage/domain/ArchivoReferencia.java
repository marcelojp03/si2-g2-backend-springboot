package com.uagrm.si2g2.storage.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "archivo_referencia", schema = "sia")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class ArchivoReferencia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "id_institucion", nullable = false)
    private UUID idInstitucion;

    @Column(name = "id_archivo", nullable = false)
    private UUID idArchivo;

    @Column(name = "modulo", nullable = false, length = 50)
    private String modulo;

    @Column(name = "entidad", nullable = false, length = 50)
    private String entidad;

    @Column(name = "id_entidad", nullable = false)
    private UUID idEntidad;

    @Column(name = "tipo_referencia", nullable = false, length = 30)
    private String tipoReferencia;

    @Builder.Default
    @Column(name = "es_principal", nullable = false)
    private Boolean esPrincipal = false;

    @Column(name = "orden_visual")
    private Integer ordenVisual;

    @Column(name = "observacion", length = 255)
    private String observacion;

    @Builder.Default
    @Column(name = "estado", nullable = false, length = 15)
    private String estado = "ACTIVO";

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @PrePersist
    protected void onCreate() {
        creadoEn = Instant.now();
        actualizadoEn = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        actualizadoEn = Instant.now();
    }
}
