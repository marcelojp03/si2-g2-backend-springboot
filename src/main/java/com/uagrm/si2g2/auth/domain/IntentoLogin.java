package com.uagrm.si2g2.auth.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "intento_login")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentoLogin {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "correo", nullable = false)
    private String correo;

    @Column(name = "id_usuario")
    private UUID idUsuario;

    @Column(name = "id_institucion")
    private UUID idInstitucion;

    @Column(name = "fecha_intento", nullable = false)
    private Instant fechaIntento;

    @Column(name = "exito", nullable = false)
    private boolean exito;

    @Column(name = "ip", length = 50)
    private String ip;

    @Column(name = "agente_usuario", columnDefinition = "TEXT")
    private String agenteUsuario;

    @Column(name = "motivo_fallo", length = 60)
    private String motivoFallo;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @PrePersist
    protected void onCreate() {
        creadoEn = Instant.now();
        if (fechaIntento == null) {
            fechaIntento = Instant.now();
        }
    }
}
