package com.uagrm.si2g2.saas.suscripcion.domain;

import com.uagrm.si2g2.common.entity.BaseEntity;
import com.uagrm.si2g2.saas.plan.domain.PlanSuscripcion;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "suscripcion_institucion")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, of = "id")
public class SuscripcionInstitucion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "id_institucion", nullable = false)
    private UUID idInstitucion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_plan", nullable = false)
    private PlanSuscripcion plan;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Builder.Default
    @Column(name = "estado", nullable = false, length = 20)
    protected String estado = "ACTIVA";

    @Builder.Default
    @Column(name = "simulada", nullable = false)
    private Boolean simulada = true;

    @Column(name = "observacion", columnDefinition = "TEXT")
    private String observacion;
}
