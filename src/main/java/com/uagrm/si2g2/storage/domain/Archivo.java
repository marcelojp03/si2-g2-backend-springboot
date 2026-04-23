package com.uagrm.si2g2.storage.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "archivo", schema = "sia")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Archivo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "id_institucion", nullable = false)
    private UUID idInstitucion;

    @Column(name = "id_usuario_subio")
    private UUID idUsuarioSubio;

    @Column(name = "nombre_original", nullable = false, length = 255)
    private String nombreOriginal;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @Column(name = "extension", length = 20)
    private String extension;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "tamano_bytes", nullable = false)
    private Long tamanoBytes;

    @Column(name = "bucket_s3", nullable = false, length = 150)
    private String bucketS3;

    @Column(name = "region_s3", length = 50)
    private String regionS3;

    @Column(name = "key_s3", nullable = false)
    private String keyS3;

    @Column(name = "etag", length = 100)
    private String etag;

    @Column(name = "checksum_sha256", length = 128)
    private String checksumSha256;

    @Column(name = "categoria", nullable = false, length = 30)
    private String categoria;

    @Builder.Default
    @Column(name = "visibilidad", nullable = false, length = 20)
    private String visibilidad = "PRIVADO";

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
