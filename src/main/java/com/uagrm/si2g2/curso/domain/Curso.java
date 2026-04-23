package com.uagrm.si2g2.curso.domain;

import com.uagrm.si2g2.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "curso")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, of = "id")
public class Curso extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "id_institucion", nullable = false)
    private UUID idInstitucion;

    @Column(name = "codigo", length = 30)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "nivel", length = 50)
    private String nivel;

    @Builder.Default
    @Column(name = "estado", nullable = false, length = 15)
    protected String estado = "ACTIVO";
}
