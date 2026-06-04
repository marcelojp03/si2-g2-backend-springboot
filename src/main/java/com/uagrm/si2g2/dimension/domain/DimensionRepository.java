package com.uagrm.si2g2.dimension.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DimensionRepository extends JpaRepository<Dimension, UUID> {

    List<Dimension> findAllByEsGlobalTrueAndEstadoOrderByNombreAsc(String estado);

    List<Dimension> findAllByIdInstitucionAndEstadoOrderByNombreAsc(UUID idInstitucion, String estado);

    @Query("SELECT d FROM Dimension d WHERE d.estado = 'ACTIVO' AND (d.esGlobal = TRUE OR d.idInstitucion = :idInstitucion) ORDER BY d.nombre ASC")
    List<Dimension> findDisponiblesParaInstitucion(@Param("idInstitucion") UUID idInstitucion);

    Optional<Dimension> findByIdInstitucionAndNombre(UUID idInstitucion, String nombre);

    Optional<Dimension> findByEsGlobalTrueAndNombre(String nombre);

    boolean existsByIdInstitucionAndNombre(UUID idInstitucion, String nombre);

    @Query("SELECT d FROM Dimension d WHERE d.estado = 'ACTIVO' AND d.esGlobal = TRUE AND d.nombre IN :nombres")
    List<Dimension> findGlobalesPorNombre(@Param("nombres") List<String> nombres);
}
