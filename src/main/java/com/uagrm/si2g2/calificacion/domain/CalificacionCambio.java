package com.uagrm.si2g2.calificacion.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "calificacion_cambio")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class CalificacionCambio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "id_institucion", nullable = false)
    private UUID idInstitucion;

    @Column(name = "id_calificacion", nullable = false)
    private UUID idCalificacion;

    @Column(name = "id_usuario")
    private UUID idUsuario;

    @Column(name = "valor_anterior", length = 30)
    private String valorAnterior;

    @Column(name = "valor_nuevo", nullable = false, length = 30)
    private String valorNuevo;

    @Column(name = "razon", nullable = false, length = 255)
    private String razon;

    @Column(name = "fecha_cambio", nullable = false)
    private Instant fechaCambio;

    @PrePersist
    protected void onCreate() {
        fechaCambio = Instant.now();
    }
}
