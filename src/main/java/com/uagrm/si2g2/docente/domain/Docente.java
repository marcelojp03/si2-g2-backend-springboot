package com.uagrm.si2g2.docente.domain;

import com.uagrm.si2g2.common.entity.BaseEntity;
import com.uagrm.si2g2.materia.domain.Materia;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "docente")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, of = "id")
public class Docente extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "id_institucion", nullable = false)
    private UUID idInstitucion;

    /** FK opcional a usuario del sistema */
    @Column(name = "id_usuario")
    private UUID idUsuario;

    @Column(name = "codigo", nullable = false, length = 30)
    private String codigo;

    @Column(name = "documento_identidad", length = 20)
    private String documentoIdentidad;

    @Column(name = "nombres", nullable = false, length = 100)
    private String nombres;

    @Column(name = "apellidos", nullable = false, length = 100)
    private String apellidos;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "correo", length = 150)
    private String correo;

    /** Resumen legible; se sincroniza con los nombres de {@link #materias}. */
    @Column(name = "especialidad", length = 150)
    private String especialidad;

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "docente_materia",
            joinColumns = @JoinColumn(name = "id_docente"),
            inverseJoinColumns = @JoinColumn(name = "id_materia")
    )
    private Set<Materia> materias = new HashSet<>();

    @Builder.Default
    @Column(name = "estado", nullable = false, length = 15)
    protected String estado = "ACTIVO";
}
