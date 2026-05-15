package com.uagrm.si2g2.dashboard.dto;

import java.time.Instant;
import java.util.List;

public record DashboardGlobalResponse(
        List<DashboardKpi> kpisGlobales,
        DashboardChart institucionesPorTipo,
        DashboardChart institucionesPorEstado,
        DashboardChart altasPorMes,
        List<DashboardAlert> alertasGlobales,
        List<DashboardRecentInstitution> institucionesRecientes,
        List<DashboardAction> accesosRapidos,
        Instant generadoEn
) {
}
