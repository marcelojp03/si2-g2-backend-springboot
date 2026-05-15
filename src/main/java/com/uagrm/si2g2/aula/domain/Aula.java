package com.uagrm.si2g2.aula.domain;

import com.uagrm.si2g2.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "aula")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, of = "id")
public class Aula extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "id_institucion", nullable = false)
    private UUID idInstitucion;

    @Column(name = "codigo", nullable = false, length = 30)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "capacidad", nullable = false)
    private Integer capacidad;

    @Column(name = "ubicacion", length = 180)
    private String ubicacion;

    @Column(name = "recursos", length = 500)
    private String recursos;

    @Builder.Default
    @Column(name = "estado", nullable = false, length = 15)
    protected String estado = "ACTIVO";
}
