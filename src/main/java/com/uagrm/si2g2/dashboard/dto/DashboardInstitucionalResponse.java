package com.uagrm.si2g2.dashboard.dto;

import com.uagrm.si2g2.academico.dto.GestionAcademicaResponse;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record DashboardInstitucionalResponse(
        DashboardInstitucionInfo institucion,
        GestionAcademicaResponse gestionActiva,
        Map<String, String> filtrosAplicados,
        List<DashboardKpi> kpis,
        List<DashboardChart> graficos,
        List<DashboardAlert> alertas,
        List<DashboardRiesgoAcademicoAlert> alertasRiesgoAcademico,
        List<DashboardAction> tareasPendientes,
        List<DashboardAction> accesosRapidos,
        List<String> modulosDisponibles,
        Instant generadoEn
) {
}
