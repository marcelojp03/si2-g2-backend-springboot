package com.uagrm.si2g2.saas.plan.domain;

import com.uagrm.si2g2.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "plan_suscripcion")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, of = "id")
public class PlanSuscripcion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "codigo", nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "max_usuarios", nullable = false)
    private Integer maxUsuarios;

    @Column(name = "max_almacenamiento_mb", nullable = false)
    private Integer maxAlmacenamientoMb;

    @Column(name = "precio_mensual", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioMensual;

    @Builder.Default
    @Column(name = "estado", nullable = false, length = 15)
    protected String estado = "ACTIVO";

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "plan_modulo",
            joinColumns = @JoinColumn(name = "id_plan"),
            inverseJoinColumns = @JoinColumn(name = "id_modulo")
    )
    @Builder.Default
    private Set<ModuloSistema> modulos = new HashSet<>();
}
