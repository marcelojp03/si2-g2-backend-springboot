package com.uagrm.si2g2.pagos.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanPagoRepository extends JpaRepository<PlanPago, UUID> {

    Optional<PlanPago> findByIdAndIdInstitucion(UUID id, UUID idInstitucion);

    List<PlanPago> findByIdInstitucionAndActivoTrueOrderByNombre(UUID idInstitucion);

    List<PlanPago> findByIdInstitucionOrderByNombre(UUID idInstitucion);

    boolean existsByIdInstitucionAndNombreIgnoreCase(UUID idInstitucion, String nombre);
}
