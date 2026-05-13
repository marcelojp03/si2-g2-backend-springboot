package com.uagrm.si2g2.seed.api;

import com.uagrm.si2g2.common.dto.ApiResponse;
import com.uagrm.si2g2.seed.application.SyntheticDataSeeder;
import com.uagrm.si2g2.seed.dto.SeedResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seed")
@RequiredArgsConstructor
public class SeedController {

    private final SyntheticDataSeeder syntheticDataSeeder;

    @PostMapping("/synthetic")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SeedResult>> generarDatosSinteticos() {
        return ResponseEntity.ok(ApiResponse.ok("Datos sinteticos generados", syntheticDataSeeder.seed()));
    }
}
