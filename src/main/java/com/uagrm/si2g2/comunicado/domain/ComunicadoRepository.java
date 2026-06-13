package com.uagrm.si2g2.comunicado.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ComunicadoRepository extends JpaRepository<Comunicado, UUID> {

    Optional<Comunicado> findByIdAndIdInstitucion(UUID id, UUID idInstitucion);

    List<Comunicado> findAllByIdInstitucionOrderByCreadoEnDesc(UUID idInstitucion);

    @Query("""
        SELECT c FROM Comunicado c
        WHERE c.idInstitucion = :idInstitucion
          AND (:estado IS NULL OR c.estado = :estado)
          AND (:tipo IS NULL OR c.tipo = :tipo)
        ORDER BY c.creadoEn DESC
    """)
    List<Comunicado> buscarConFiltros(
            @Param("idInstitucion") UUID idInstitucion,
            @Param("estado") String estado,
            @Param("tipo") String tipo,
            Pageable pageable);

    @Query("""
        SELECT c FROM Comunicado c
        WHERE c.idInstitucion = :idInstitucion
          AND c.estado = 'PUBLICADO'
          AND (:tipo IS NULL OR c.tipo = :tipo)
        ORDER BY c.publicadoEn DESC
    """)
    List<Comunicado> findPublicados(
            @Param("idInstitucion") UUID idInstitucion,
            @Param("tipo") String tipo,
            Pageable pageable);

    long countByIdInstitucionAndEstado(UUID idInstitucion, String estado);
}
