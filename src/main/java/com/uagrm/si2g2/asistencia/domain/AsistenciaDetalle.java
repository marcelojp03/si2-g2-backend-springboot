package com.uagrm.si2g2.asistencia.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "asistencia_detalle")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class AsistenciaDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "id_asistencia_registro", nullable = false)
    private UUID idAsistenciaRegistro;

    @Column(name = "id_inscripcion", nullable = false)
    private UUID idInscripcion;

    @Column(name = "estado_asistencia", nullable = false, length = 15)
    private String estadoAsistencia;

    @Column(name = "justificacion", length = 500)
    private String justificacion;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @PrePersist
    protected void onCreate() {
        creadoEn = Instant.now();
        actualizadoEn = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        actualizadoEn = Instant.now();
    }
}