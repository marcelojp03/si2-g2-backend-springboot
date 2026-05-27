package com.uagrm.si2g2.calificacion.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * ====================================================================
 * ENTIDAD: EvaluacionMateria (NUEVA)
 * ====================================================================
 * 
 * Representa una evaluación a nivel de MATERIA (no de asignación docente).
 * Permite que todos los estudiantes de una materia en un período académico
 * se evalúen con la misma estructura.
 * 
 * CARACTERÍSTICAS:
 * - Vinculada a Materia + Período (no a Asignación Docente)
 * - Una estructura de evaluaciones única por materia/período
 * - Todos los paralelos de esa materia usan las mismas evaluaciones
 * - Soporta escalas numéricas (0-100) y literales (A-F)
 * - Tiene ponderación (peso %) para cálculo de nota final
 * - Estados: ABIERTA (recibe notas), CERRADA (no modifica), ANULADA (excluida)
 * 
 * CAMPOS CLAVE:
 * - idInstitucion: Aislamiento multitenencia (obligatorio)
 * - idMateria: Referencia a la materia (obligatorio)
 * - periodo: Período académico (1-6)
 * - ponderacion: Peso en porcentaje (debe sumar ≤100% por materia/período)
 * - escala: NUMERICA (0-100) o LITERAL (A-F)
 * 
 * AUDITORIA:
 * - creadoPor: ID del usuario que creó
 * - creadoEn, actualizadoEn: Timestamps automáticos
 */
@Entity
@Table(name = "evaluacion_materia", uniqueConstraints = {
        @UniqueConstraint(name = "uk_eval_materia_periodo_nombre", columnNames = { "id_institucion", "id_materia",
                "periodo", "nombre" })
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class EvaluacionMateria {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Institución a la que pertenece (multitenencia) */
    @Column(name = "id_institucion", nullable = false)
    private UUID idInstitucion;

    /** Materia a la que pertenece esta evaluación */
    @Column(name = "id_materia", nullable = false)
    private UUID idMateria;

    /** Usuario que creó la evaluación (auditoría) */
    @Column(name = "creado_por")
    private UUID creadoPor;

    /** Período académico: 1, 2, 3, 4, 5 ó 6 */
    @Column(name = "periodo", nullable = false)
    private Integer periodo;

    /**
     * Tipo de evaluación: PARCIAL, EXAMEN, TRABAJO_PRACTICO, PROYECTO,
     * PARTICIPACION
     */
    @Column(name = "tipo", nullable = false, length = 40)
    private String tipo;

    /** Nombre descriptivo: "Parcial 1", "Proyecto Final", etc. */
    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    /** Ponderación/peso en porcentaje (0.01-100.00) */
    @Column(name = "ponderacion", nullable = false, precision = 5, scale = 2)
    private BigDecimal ponderacion;

    /** Escala de calificación: NUMERICA (0-100) o LITERAL (A-F) */
    @Builder.Default
    @Column(name = "escala", nullable = false, length = 15)
    private String escala = "NUMERICA";

    /** Estado: ABIERTA, CERRADA, ANULADA */
    @Builder.Default
    @Column(name = "estado", nullable = false, length = 15)
    private String estado = "ABIERTA";

    /** Timestamp de creación (inmutable) */
    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    /** Timestamp de última actualización */
    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @PrePersist
    protected void onCreate() {
        Instant ahora = Instant.now();
        creadoEn = ahora;
        actualizadoEn = ahora;
    }

    @PreUpdate
    protected void onUpdate() {
        actualizadoEn = Instant.now();
    }
}
