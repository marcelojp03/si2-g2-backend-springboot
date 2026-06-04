package com.uagrm.si2g2.dimension.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PeriodoDimensionRepository extends JpaRepository<PeriodoDimension, UUID> {

    List<PeriodoDimension> findAllByIdPeriodoEvaluacion(UUID idPeriodoEvaluacion);

    Optional<PeriodoDimension> findByIdPeriodoEvaluacionAndDimensionId(UUID idPeriodoEvaluacion, UUID idDimension);

    void deleteAllByIdPeriodoEvaluacion(UUID idPeriodoEvaluacion);

    boolean existsByIdPeriodoEvaluacionAndDimensionId(UUID idPeriodoEvaluacion, UUID idDimension);
}
