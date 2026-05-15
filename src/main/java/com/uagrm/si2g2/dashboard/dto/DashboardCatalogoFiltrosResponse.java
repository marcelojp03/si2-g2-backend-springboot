package com.uagrm.si2g2.dashboard.dto;

import java.util.List;

public record DashboardCatalogoFiltrosResponse(
        List<DashboardFilterOption> gestiones,
        List<DashboardFilterOption> cursos,
        List<DashboardFilterOption> paralelos,
        List<DashboardFilterOption> materias,
        List<DashboardFilterOption> turnos,
        List<DashboardFilterOption> periodos
) {
}
