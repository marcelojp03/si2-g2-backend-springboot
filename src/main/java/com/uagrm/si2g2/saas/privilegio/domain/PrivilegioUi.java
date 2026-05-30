package com.uagrm.si2g2.saas.privilegio.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "privilegio_ui",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"id_institucion", "id_rol", "modulo", "entidad", "campo"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class PrivilegioUi {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "id_institucion", nullable = false)
    private UUID idInstitucion;

    @Column(name = "id_rol", nullable = false)
    private UUID idRol;

    @Column(name = "modulo", nullable = false, length = 60)
    private String modulo;

    @Column(name = "entidad", nullable = false, length = 60)
    private String entidad;

    @Column(name = "campo", nullable = false, length = 100)
    private String campo;

    /** VISIBLE | OCULTO */
    @Builder.Default
    @Column(name = "visibilidad", nullable = false, length = 20)
    private String visibilidad = "VISIBLE";

    /** EDITABLE | SOLO_LECTURA | OCULTO */
    @Builder.Default
    @Column(name = "edicion", nullable = false, length = 20)
    private String edicion = "EDITABLE";

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
