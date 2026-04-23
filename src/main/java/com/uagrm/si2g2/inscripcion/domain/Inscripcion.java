package com.uagrm.si2g2.inscripcion.domain;

import com.uagrm.si2g2.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "inscripcion")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, of = "id")
public class Inscripcion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "id_institucion", nullable = false)
    private UUID idInstitucion;

    @Column(name = "id_estudiante", nullable = false)
    private UUID idEstudiante;

    @Column(name = "id_gestion", nullable = false)
    private UUID idGestion;

    @Column(name = "id_curso", nullable = false)
    private UUID idCurso;

    @Column(name = "id_paralelo", nullable = false)
    private UUID idParalelo;

    @Column(name = "fecha_inscripcion", nullable = false)
    private LocalDate fechaInscripcion;

    @Builder.Default
    @Column(name = "estado", nullable = false, length = 15)
    protected String estado = "ACTIVO";
}
