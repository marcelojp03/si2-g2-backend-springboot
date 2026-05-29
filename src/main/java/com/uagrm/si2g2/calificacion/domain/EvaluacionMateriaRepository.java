package com.uagrm.si2g2.calificacion.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EvaluacionMateriaRepository extends JpaRepository<EvaluacionMateria, UUID> {

    Optional<EvaluacionMateria> findByIdAndIdInstitucion(UUID id, UUID idInstitucion);

    List<EvaluacionMateria> findAllByIdInstitucionAndIdMateria(UUID idInstitucion, UUID idMateria);

    List<EvaluacionMateria> findAllByIdInstitucionAndIdMateriaAndPeriodo(
            UUID idInstitucion,
            UUID idMateria,
            Integer periodo);

    boolean existsByIdInstitucionAndIdMateriaAndPeriodoAndNombreIgnoreCase(
            UUID idInstitucion,
            UUID idMateria,
            Integer periodo,
            String nombre);

    @Query("""
            select coalesce(sum(e.ponderacion), 0)
            from EvaluacionMateria e
            where e.idInstitucion = :idInstitucion
              and e.idMateria = :idMateria
              and e.periodo = :periodo
              and e.estado <> 'ANULADA'
              and (:idExcluir is null or e.id <> :idExcluir)
            """)
    BigDecimal sumPonderacionActiva(@Param("idInstitucion") UUID idInstitucion,
            @Param("idMateria") UUID idMateria,
            @Param("periodo") Integer periodo,
            @Param("idExcluir") UUID idExcluir);
}
