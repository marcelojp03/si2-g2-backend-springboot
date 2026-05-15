package com.uagrm.si2g2.dashboard.dto;

import java.util.List;

public record DashboardChart(
        String codigo,
        String titulo,
        String tipo,
        List<String> labels,
        List<DashboardDataset> datasets
) {
}
