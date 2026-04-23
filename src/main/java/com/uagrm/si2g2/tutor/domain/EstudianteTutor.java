package com.uagrm.si2g2.tutor.domain;

import com.uagrm.si2g2.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "estudiante_tutor")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, of = "id")
public class EstudianteTutor extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "id_institucion", nullable = false)
    private UUID idInstitucion;

    @Column(name = "id_estudiante", nullable = false)
    private UUID idEstudiante;

    @Column(name = "id_tutor", nullable = false)
    private UUID idTutor;

    @Column(name = "parentesco", length = 50)
    private String parentesco;

    @Builder.Default
    @Column(name = "es_principal", nullable = false)
    private boolean esPrincipal = false;

    @Builder.Default
    @Column(name = "estado", nullable = false, length = 15)
    protected String estado = "ACTIVO";
}
