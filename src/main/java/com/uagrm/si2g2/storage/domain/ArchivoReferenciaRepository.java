package com.uagrm.si2g2.storage.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArchivoReferenciaRepository extends JpaRepository<ArchivoReferencia, UUID> {

    List<ArchivoReferencia> findByIdInstitucionAndModuloAndEntidadAndIdEntidadAndEstado(
            UUID idInstitucion, String modulo, String entidad, UUID idEntidad, String estado);

    Optional<ArchivoReferencia> findByIdInstitucionAndModuloAndEntidadAndIdEntidadAndTipoReferenciaAndEsPrincipalTrueAndEstado(
            UUID idInstitucion, String modulo, String entidad, UUID idEntidad, String tipoReferencia, String estado);

    @Modifying
    @Query("""
            UPDATE ArchivoReferencia ar
            SET ar.esPrincipal = false, ar.estado = 'INACTIVO'
            WHERE ar.idInstitucion = :idInstitucion
              AND ar.modulo = :modulo
              AND ar.entidad = :entidad
              AND ar.idEntidad = :idEntidad
              AND ar.tipoReferencia = :tipoReferencia
              AND ar.esPrincipal = true
              AND ar.estado = 'ACTIVO'
            """)
    void desactivarPrincipalActual(
            @Param("idInstitucion") UUID idInstitucion,
            @Param("modulo") String modulo,
            @Param("entidad") String entidad,
            @Param("idEntidad") UUID idEntidad,
            @Param("tipoReferencia") String tipoReferencia);
}
