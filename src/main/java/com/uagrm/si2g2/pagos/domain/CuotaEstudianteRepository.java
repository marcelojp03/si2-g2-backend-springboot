package com.uagrm.si2g2.pagos.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CuotaEstudianteRepository extends JpaRepository<CuotaEstudiante, UUID> {

    List<CuotaEstudiante> findByIdEstudianteOrderByNumeroCuota(UUID idEstudiante);

    List<CuotaEstudiante> findByIdEstudianteAndIdGestionAcademicaOrderByNumeroCuota(UUID idEstudiante, UUID idGestion);

    List<CuotaEstudiante> findByIdInstitucionAndEstado(UUID idInstitucion, String estado);

    List<CuotaEstudiante> findByIdInstitucionAndIdPlanPago(UUID idInstitucion, UUID idPlan);

    long countByIdEstudianteAndIdGestionAcademicaAndEstado(UUID idEstudiante, UUID idGestion, String estado);

    List<CuotaEstudiante> findByFechaVencimientoBeforeAndEstado(LocalDate fecha, String estado);

    boolean existsByIdEstudianteAndIdPlanPagoAndNumeroCuota(UUID idEstudiante, UUID idPlan, Integer numero);
}
