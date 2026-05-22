package com.uagrm.si2g2.calificacion.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EvaluacionRepository extends JpaRepository<Evaluacion, UUID> {

    Optional<Evaluacion> findByIdAndIdInstitucion(UUID id, UUID idInstitucion);

    List<Evaluacion> findAllByIdInstitucionAndIdAsignacionDocente(UUID idInstitucion, UUID idAsignacionDocente);

    List<Evaluacion> findAllByIdInstitucionAndIdAsignacionDocenteAndPeriodo(
            UUID idInstitucion,
            UUID idAsignacionDocente,
            Integer periodo
    );

    boolean existsByIdInstitucionAndIdAsignacionDocenteAndPeriodoAndNombreIgnoreCase(
            UUID idInstitucion,
            UUID idAsignacionDocente,
            Integer periodo,
            String nombre
    );

    @Query("""
            select coalesce(sum(e.ponderacion), 0)
            from Evaluacion e
            where e.idInstitucion = :idInstitucion
              and e.idAsignacionDocente = :idAsignacionDocente
              and e.periodo = :periodo
              and e.estado <> 'ANULADA'
              and (:idExcluir is null or e.id <> :idExcluir)
            """)
    BigDecimal sumPonderacionActiva(@Param("idInstitucion") UUID idInstitucion,
                                    @Param("idAsignacionDocente") UUID idAsignacionDocente,
                                    @Param("periodo") Integer periodo,
                                    @Param("idExcluir") UUID idExcluir);
}
