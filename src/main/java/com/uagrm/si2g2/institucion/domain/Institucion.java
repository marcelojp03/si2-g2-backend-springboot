package com.uagrm.si2g2.institucion.domain;

import com.uagrm.si2g2.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "institucion")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, of = "id")
public class Institucion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "codigo", nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "tipo_institucion", nullable = false, length = 20)
    private String tipoInstitucion;

    @Column(name = "telefono", length = 30)
    private String telefono;

    @Column(name = "correo")
    private String correo;

    @Column(name = "direccion", length = 255)
    private String direccion;

    @Column(name = "logo_url")
    private String logoUrl;

    @Builder.Default
    @Column(name = "estado", nullable = false, length = 15)
    protected String estado = "ACTIVO";
}
