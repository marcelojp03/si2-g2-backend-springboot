package com.uagrm.si2g2.dashboard.api;

import com.uagrm.si2g2.common.dto.ApiResponse;
import com.uagrm.si2g2.dashboard.application.DashboardService;
import com.uagrm.si2g2.dashboard.dto.DashboardGlobalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DashboardGlobalResponse>> obtener() {
        return ResponseEntity.ok(ApiResponse.ok("Dashboard global", dashboardService.getGlobalDashboard()));
    }
}
