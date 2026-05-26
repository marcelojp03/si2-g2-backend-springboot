package com.uagrm.si2g2.saas.plan.domain;

import com.uagrm.si2g2.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "modulo_sistema")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, of = "id")
public class ModuloSistema extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "codigo", nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "icono", length = 60)
    private String icono;

    @Column(name = "ruta_frontend", length = 120)
    private String rutaFrontend;

    @Column(name = "orden_visual", nullable = false)
    @Builder.Default
    private Integer ordenVisual = 0;

    @Builder.Default
    @Column(name = "estado", nullable = false, length = 15)
    protected String estado = "ACTIVO";
}
