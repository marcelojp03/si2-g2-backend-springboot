package com.uagrm.si2g2.alertas.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertaRiesgoRepository extends JpaRepository<AlertaRiesgo, UUID> {

    Optional<AlertaRiesgo> findByIdAndIdInstitucion(UUID id, UUID idInstitucion);

    List<AlertaRiesgo> findByIdInstitucionAndActivaTrueOrderByProcesadoEnDesc(UUID idInstitucion);

    List<AlertaRiesgo> findByIdInstitucionAndIdGestionAcademicaAndActivaTrue(UUID idInstitucion, UUID idGestion);

    @Query("""
        SELECT a FROM AlertaRiesgo a
        WHERE a.idInstitucion = :idInstitucion
          AND (:idGestion IS NULL OR a.idGestionAcademica = :idGestion)
          AND (:nivel IS NULL OR a.nivelRiesgo = :nivel)
          AND a.activa = true
        ORDER BY a.procesadoEn DESC
    """)
    List<AlertaRiesgo> buscarConFiltros(
            @Param("idInstitucion") UUID idInstitucion,
            @Param("idGestion") UUID idGestion,
            @Param("nivel") String nivel);

    List<AlertaRiesgo> findByIdEstudianteAndActivaTrue(UUID idEstudiante);

    Optional<AlertaRiesgo> findTopByIdEstudianteAndIdGestionAcademicaAndActivaTrueOrderByProcesadoEnDesc(
            UUID idEstudiante, UUID idGestionAcademica);

    long countByIdInstitucionAndIdGestionAcademicaAndNivelRiesgoAndActivaTrue(
            UUID idInstitucion, UUID idGestionAcademica, String nivelRiesgo);

    @Modifying
    @Query("UPDATE AlertaRiesgo a SET a.activa = false, a.actualizadoEn = CURRENT_TIMESTAMP WHERE a.idInstitucion = :idInst AND a.idGestionAcademica = :idGes AND a.activa = true")
    int desactivarPorGestion(@Param("idInst") UUID idInst, @Param("idGes") UUID idGes);
}
