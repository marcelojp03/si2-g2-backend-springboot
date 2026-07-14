package com.uagrm.si2g2.calificacion.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ====================================================================
 * REPOSITORIO: CalificacionRepository
 * ====================================================================
 * 
 * Interfaz para acceso a datos de la entidad Calificacion.
 * Hereda CRUD básico (save, findById, delete) de JpaRepository.
 * Añade queries especializadas para búsquedas por evaluación e inscripción.
 * 
 * NOTAS:
 * - No necesita @Repository: JpaRepository lo registra automáticamente
 * - Spring genera la implementación en tiempo de ejecución
 * - El aislamiento multitenencia está en CalificacionService (filtros)
 * - Las búsquedas son case-insensitive para strings
 * 
 * MÉTODOS:
 * - findAllByIdEvaluacion: Todas las notas de una evaluación
 * - findAllByIdEvaluacionIn: Notas de múltiples evaluaciones (batch)
 * - findByIdEvaluacionAndIdInscripcion: Nota específica estudiante-evaluación
 */
public interface CalificacionRepository extends JpaRepository<Calificacion, UUID> {

    /**
     * Obtiene todas las calificaciones de una evaluación específica.
     * Útil para generar reportes o cargar una plantilla de notas.
     * 
     * @param idEvaluacion UUID de la evaluación
     * @return Lista de calificaciones (vacía si no hay notas registradas)
     */
    List<Calificacion> findAllByIdEvaluacion(UUID idEvaluacion);

    /**
     * Obtiene calificaciones de múltiples evaluaciones en una sola query.
     * Optimización para cargar notas de un conjunto de evaluaciones (ej: todas
     * las del período).
     * 
     * @param idsEvaluacion Colección de UUIDs de evaluaciones
     * @return Lista de calificaciones de todas las evaluaciones indicadas
     */
    List<Calificacion> findAllByIdEvaluacionIn(Collection<UUID> idsEvaluacion);

    /**
     * Obtiene la calificación de un estudiante en una evaluación específica.
     * Query 1:1 que identifica si ya existe una nota registrada.
     * 
     * UTILIDAD:
     * - Validar si ya existe nota (antes de crear vs actualizar)
     * - Cargar nota existente para modificarla
     * 
     * @param idEvaluacion  UUID de la evaluación
     * @param idInscripcion UUID de la inscripción del estudiante
     * @return Optional con la calificación si existe, vacío si es nuevo
     */
    Optional<Calificacion> findByIdEvaluacionAndIdInscripcion(UUID idEvaluacion, UUID idInscripcion);

    List<Calificacion> findAllByIdInscripcion(UUID idInscripcion);
}
