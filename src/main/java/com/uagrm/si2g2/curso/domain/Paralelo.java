package com.uagrm.si2g2.curso.domain;

import com.uagrm.si2g2.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "paralelo")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, of = "id")
public class Paralelo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "id_institucion", nullable = false)
    private UUID idInstitucion;

    @Column(name = "id_curso", nullable = false)
    private UUID idCurso;

    @Column(name = "id_gestion", nullable = false)
    private UUID idGestion;

    @Column(name = "nombre", nullable = false, length = 20)
    private String nombre;

    @Column(name = "capacidad")
    private Integer capacidad;

    @Builder.Default
    @Column(name = "estado", nullable = false, length = 15)
    protected String estado = "ACTIVO";
}
