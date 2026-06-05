package com.uagrm.si2g2.dimension.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "solicitud_eliminacion_dimension", schema = "sia",
        uniqueConstraints = @UniqueConstraint(name = "uq_solicitud_dimension_periodo",
                columnNames = {"id_periodo_evaluacion", "id_dimension"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class SolicitudEliminacionDimension {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "id_institucion", nullable = false)
    private UUID idInstitucion;

    @Column(name = "id_periodo_evaluacion", nullable = false)
    private UUID idPeriodoEvaluacion;

    @Column(name = "id_dimension", nullable = false)
    private UUID idDimension;

    @Builder.Default
    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "PENDIENTE";

    @Column(name = "id_usuario_solicitud", nullable = false)
    private UUID idUsuarioSolicitud;

    @Builder.Default
    @Column(name = "fecha_solicitud", nullable = false)
    private Instant fechaSolicitud = Instant.now();

    @Column(name = "id_usuario_resolucion")
    private UUID idUsuarioResolucion;

    @Column(name = "fecha_resolucion")
    private Instant fechaResolucion;

    @Column(name = "observacion", columnDefinition = "TEXT")
    private String observacion;
}
