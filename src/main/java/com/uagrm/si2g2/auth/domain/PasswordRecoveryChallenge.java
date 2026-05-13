package com.uagrm.si2g2.auth.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "password_recovery_challenge")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordRecoveryChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "id_usuario", nullable = false)
    private UUID idUsuario;

    @Column(name = "correo", nullable = false)
    private String correo;

    @Column(name = "codigo_verificacion", nullable = false, length = 6)
    private String codigoVerificacion;

    @Column(name = "token_recuperacion", length = 120)
    private String tokenRecuperacion;

    @Column(name = "intentos_verificacion", nullable = false)
    @Builder.Default
    private int intentosVerificacion = 0;

    @Column(name = "verificado", nullable = false)
    @Builder.Default
    private boolean verificado = false;

    @Column(name = "usado", nullable = false)
    @Builder.Default
    private boolean usado = false;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    @Column(name = "verificado_en")
    private Instant verificadoEn;

    @Column(name = "usado_en")
    private Instant usadoEn;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @PrePersist
    protected void onCreate() {
        creadoEn = Instant.now();
    }
}
