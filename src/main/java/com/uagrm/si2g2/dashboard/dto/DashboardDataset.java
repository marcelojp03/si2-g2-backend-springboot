package com.uagrm.si2g2.dashboard.dto;

import java.util.List;

public record DashboardDataset(
        String label,
        List<Long> data,
        String color
) {
}
