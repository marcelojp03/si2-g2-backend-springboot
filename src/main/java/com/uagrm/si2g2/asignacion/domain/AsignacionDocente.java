package com.uagrm.si2g2.asignacion.domain;

import com.uagrm.si2g2.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "asignacion_docente")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, of = "id")
public class AsignacionDocente extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "id_institucion", nullable = false)
    private UUID idInstitucion;

    @Column(name = "id_docente", nullable = false)
    private UUID idDocente;

    @Column(name = "id_materia", nullable = false)
    private UUID idMateria;

    @Column(name = "id_paralelo", nullable = false)
    private UUID idParalelo;

    @Column(name = "id_gestion", nullable = false)
    private UUID idGestion;

    @Builder.Default
    @Column(name = "estado", nullable = false, length = 15)
    protected String estado = "ACTIVO";
}
