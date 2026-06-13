package com.uagrm.si2g2.notificacion.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notificacion")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "id_institucion", nullable = false)
    private UUID idInstitucion;

    @Column(name = "id_usuario", nullable = false)
    private UUID idUsuario;

    @Column(name = "titulo", nullable = false, length = 200)
    private String titulo;

    @Column(name = "mensaje", columnDefinition = "TEXT")
    private String mensaje;

    @Builder.Default
    @Column(name = "tipo", nullable = false, length = 30)
    private String tipo = "SISTEMA";

    @Column(name = "referencia_tipo", length = 50)
    private String referenciaTipo;

    @Column(name = "referencia_id")
    private UUID referenciaId;

    @Builder.Default
    @Column(name = "leida", nullable = false)
    private Boolean leida = false;

    @Column(name = "leida_en")
    private Instant leidaEn;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @PrePersist
    protected void onCreate() {
        creadoEn = Instant.now();
    }
}
