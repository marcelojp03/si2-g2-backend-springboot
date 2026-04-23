package com.uagrm.si2g2.institucion.domain;

import com.uagrm.si2g2.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "configuracion_institucion")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, of = "id")
public class ConfiguracionInstitucion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "id_institucion", nullable = false)
    private UUID idInstitucion;

    @Column(name = "clave", nullable = false, length = 100)
    private String clave;

    @Column(name = "valor", nullable = false)
    private String valor;

    @Builder.Default
    @Column(name = "tipo_valor", nullable = false, length = 30)
    private String tipoValor = "TEXTO";

    @Column(name = "descripcion", length = 255)
    private String descripcion;
}
