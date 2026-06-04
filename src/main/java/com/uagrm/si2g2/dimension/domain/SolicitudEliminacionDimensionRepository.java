package com.uagrm.si2g2.dimension.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SolicitudEliminacionDimensionRepository extends JpaRepository<SolicitudEliminacionDimension, UUID> {

    List<SolicitudEliminacionDimension> findAllByEstadoOrderByFechaSolicitudDesc(String estado);

    List<SolicitudEliminacionDimension> findAllByIdInstitucionAndEstadoOrderByFechaSolicitudDesc(
            UUID idInstitucion, String estado);

    List<SolicitudEliminacionDimension> findAllByIdPeriodoEvaluacion(UUID idPeriodoEvaluacion);

    boolean existsByIdPeriodoEvaluacionAndIdDimensionAndEstado(
            UUID idPeriodoEvaluacion, UUID idDimension, String estado);
}
