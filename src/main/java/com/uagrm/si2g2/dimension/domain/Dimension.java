package com.uagrm.si2g2.dimension.domain;

import com.uagrm.si2g2.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "dimension", schema = "sia",
        uniqueConstraints = @UniqueConstraint(name = "uq_dimension_institucion_nombre",
                columnNames = {"id_institucion", "nombre"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, of = "id")
public class Dimension extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "id_institucion")
    private UUID idInstitucion;

    @Column(name = "nombre", nullable = false, length = 50)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Builder.Default
    @Column(name = "peso_default", nullable = false)
    private Integer pesoDefault = 0;

    @Builder.Default
    @Column(name = "estado", nullable = false, length = 20)
    protected String estado = "ACTIVO";

    @Builder.Default
    @Column(name = "es_global", nullable = false)
    private Boolean esGlobal = false;
}
