package com.uagrm.si2g2.dashboard.api;

import com.uagrm.si2g2.common.dto.ApiResponse;
import com.uagrm.si2g2.dashboard.application.DashboardService;
import com.uagrm.si2g2.dashboard.dto.DashboardAlert;
import com.uagrm.si2g2.dashboard.dto.DashboardCatalogoFiltrosResponse;
import com.uagrm.si2g2.dashboard.dto.DashboardInstitucionalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DashboardInstitucionalResponse>> obtenerDashboard(@RequestParam Map<String, String> filtros) {
        return ResponseEntity.ok(ApiResponse.ok("Dashboard institucional", dashboardService.getInstitutionalDashboard(filtros)));
    }

    @GetMapping("/catalogo-filtros")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DashboardCatalogoFiltrosResponse>> catalogoFiltros() {
        return ResponseEntity.ok(ApiResponse.ok("Catalogo de filtros", dashboardService.getCatalogoFiltros()));
    }

    @GetMapping("/alertas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<DashboardAlert>>> alertas(
            @RequestParam(required = false) String severidad,
            @RequestParam(required = false) String modulo,
            @RequestParam(required = false) String estado) {
        return ResponseEntity.ok(ApiResponse.ok("Alertas del dashboard",
                dashboardService.getInstitutionalAlerts(severidad, modulo, estado)));
    }
}
