package com.uagrm.si2g2.calificacion.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * ====================================================================
 * ENTIDAD: Calificacion
 * ====================================================================
 * 
 * Representa la nota de un estudiante en una evaluación específica.
 * Es el registro individual que vincula:
 * - Un estudiante (a través de su inscripción)
 * - Una evaluación específica
 * - La nota obtenida (numérica o literal)
 * 
 * CARACTERÍSTICAS:
 * - Una nota por estudiante-evaluación (constraint UNIQUE)
 * - Soporta actualizaciones (con auditoría en CalificacionCambio)
 * - Nota numérica: BigDecimal con 2 decimales (ej: 85.50)
 * - Nota literal: Texto de 5 caracteres máximo (ej: "A", "B", "C", "D", "F")
 * 
 * CAMPOS CLAVE:
 * - idInstitucion: Multitenencia (mismo de Evaluacion)
 * - idEvaluacion: FK a la evaluación (vincula con su periodo/tipo/ponderación)
 * - idInscripcion: FK a la inscripción del estudiante
 * - registradoPor: Usuario que registró la nota (auditoría)
 * 
 * AUDITORÍA:
 * - creadoEn: Timestamp de registro inicial
 * - actualizadoEn: Timestamp de última modificación
 * - Cambios: Se registran en CalificacionCambio con razón del cambio
 */
@Entity
@Table(name = "calificacion")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Calificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Institución a la que pertenece (multitenencia) */
    @Column(name = "id_institucion", nullable = false)
    private UUID idInstitucion;

    /** Evaluación a la que corresponde la nota */
    @Column(name = "id_evaluacion", nullable = false)
    private UUID idEvaluacion;

    /** Inscripción del estudiante (vincula con id_estudiante) */
    @Column(name = "id_inscripcion", nullable = false)
    private UUID idInscripcion;

    /** Usuario que registró/modificó la nota (auditoría) */
    @Column(name = "registrado_por")
    private UUID registradoPor;

    /** Nota numérica: 0 a 100 con 2 decimales (usado si escala es NUMERICA) */
    @Column(name = "nota_numerica", precision = 8, scale = 2)
    private BigDecimal notaNumerica;

    /** Nota literal: A, B, C, D, F (usado si escala es LITERAL) */
    @Column(name = "nota_literal", length = 5)
    private String notaLiteral;

    /** Timestamp de creación (inmutable) */
    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    /** Timestamp de última actualización */
    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    /**
     * Callback JPA: Se ejecuta antes de guardar la entidad por primera vez.
     * Inicializa timestamps de creación y actualización.
     */
    @PrePersist
    protected void onCreate() {
        creadoEn = Instant.now();
        actualizadoEn = Instant.now();
    }

    /**
     * Callback JPA: Se ejecuta antes de actualizar la entidad.
     * Actualiza el timestamp de modificación.
     */
    @PreUpdate
    protected void onUpdate() {
        actualizadoEn = Instant.now();
    }
}
