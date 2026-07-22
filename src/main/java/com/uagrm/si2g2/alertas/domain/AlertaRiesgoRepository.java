package com.uagrm.si2g2.alertas.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertaRiesgoRepository extends JpaRepository<AlertaRiesgo, UUID> {

    Optional<AlertaRiesgo> findByIdAndIdInstitucion(UUID id, UUID idInstitucion);

    @Query("select a.idEstudiante from AlertaRiesgo a where a.id = :id and a.idInstitucion = :idInstitucion")
    Optional<UUID> findIdEstudianteByIdAndIdInstitucion(
            @Param("id") UUID id, @Param("idInstitucion") UUID idInstitucion);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AlertaRiesgo a where a.id = :id and a.idInstitucion = :idInstitucion")
    Optional<AlertaRiesgo> findByIdAndIdInstitucionForUpdate(
            @Param("id") UUID id, @Param("idInstitucion") UUID idInstitucion);

    List<AlertaRiesgo> findByIdInstitucionAndActivaTrueOrderByProcesadoEnDesc(UUID idInstitucion);

    List<AlertaRiesgo> findByIdInstitucionAndIdGestionAcademicaAndActivaTrue(UUID idInstitucion, UUID idGestion);

    @Query("""
        SELECT a FROM AlertaRiesgo a
        WHERE a.idInstitucion = :idInstitucion
          AND (:idGestion IS NULL OR a.idGestionAcademica = :idGestion)
          AND (:nivel IS NULL OR a.nivelRiesgo = :nivel)
          AND (:activa IS NULL OR a.activa = :activa)
        ORDER BY a.procesadoEn DESC
    """)
    List<AlertaRiesgo> buscarConFiltros(
            @Param("idInstitucion") UUID idInstitucion,
            @Param("idGestion") UUID idGestion,
            @Param("nivel") String nivel,
            @Param("activa") Boolean activa);

    @Query("""
        SELECT a FROM AlertaRiesgo a
        WHERE a.idInstitucion = :idInstitucion
          AND (:idGestion IS NULL OR a.idGestionAcademica = :idGestion)
          AND (:nivel IS NULL OR a.nivelRiesgo = :nivel)
          AND (:activa IS NULL OR a.activa = :activa)
          AND EXISTS (
              SELECT i.id FROM Inscripcion i, Paralelo p, AsignacionDocente ad
              WHERE i.idInstitucion = a.idInstitucion
                AND i.idEstudiante = a.idEstudiante
                AND i.idGestion = a.idGestionAcademica
                AND i.estado = 'ACTIVA'
                AND p.id = i.idParalelo
                AND p.idInstitucion = a.idInstitucion
                AND ad.idInstitucion = a.idInstitucion
                AND ad.idGestion = a.idGestionAcademica
                AND ad.idParalelo = p.id
                AND ad.estado = 'ACTIVA'
                AND (:idCurso IS NULL OR p.idCurso = :idCurso)
                AND (:idParalelo IS NULL OR p.id = :idParalelo)
                AND (:idMateria IS NULL OR ad.idMateria = :idMateria)
          )
        ORDER BY a.procesadoEn DESC
    """)
    List<AlertaRiesgo> buscarConFiltrosAcademicos(
            @Param("idInstitucion") UUID idInstitucion,
            @Param("idGestion") UUID idGestion,
            @Param("idCurso") UUID idCurso,
            @Param("idParalelo") UUID idParalelo,
            @Param("idMateria") UUID idMateria,
            @Param("nivel") String nivel,
            @Param("activa") Boolean activa);

    List<AlertaRiesgo> findByIdInstitucionAndIdEstudianteAndActivaTrue(UUID idInstitucion, UUID idEstudiante);

    Optional<AlertaRiesgo> findByIdInstitucionAndIdEstudianteAndIdGestionAcademicaAndActivaTrue(
            UUID idInstitucion, UUID idEstudiante, UUID idGestionAcademica);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select a from AlertaRiesgo a
            where a.idInstitucion = :idInstitucion
              and a.idEstudiante = :idEstudiante
              and a.idGestionAcademica = :idGestion
              and a.activa = true
            """)
    Optional<AlertaRiesgo> findActivaForUpdate(
            @Param("idInstitucion") UUID idInstitucion,
            @Param("idEstudiante") UUID idEstudiante,
            @Param("idGestion") UUID idGestion);

    List<AlertaRiesgo> findByIdInstitucionAndIdGestionAcademicaAndNivelRiesgoInAndActivaTrueOrderByProcesadoEnDesc(
            UUID idInstitucion, UUID idGestionAcademica, List<String> niveles);

    long countByIdInstitucionAndIdGestionAcademicaAndNivelRiesgoAndActivaTrue(
            UUID idInstitucion, UUID idGestionAcademica, String nivelRiesgo);

}
