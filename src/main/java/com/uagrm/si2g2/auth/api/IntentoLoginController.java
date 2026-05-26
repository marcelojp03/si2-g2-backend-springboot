package com.uagrm.si2g2.auth.api;

import com.uagrm.si2g2.auth.domain.IntentoLogin;
import com.uagrm.si2g2.auth.domain.IntentoLoginRepository;
import com.uagrm.si2g2.auth.dto.IntentoLoginResponse;
import com.uagrm.si2g2.common.dto.ApiResponse;
import com.uagrm.si2g2.common.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth/intentos-login")
@RequiredArgsConstructor
public class IntentoLoginController {

    private final IntentoLoginRepository intentoRepo;

    /**
     * Consulta intentos de login con filtros básicos.
     * SUPER_ADMIN puede consultar cualquier institución; ADMIN_INSTITUCION solo la suya.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_INSTITUCION', 'DIRECTOR')")
    public ResponseEntity<ApiResponse<List<IntentoLoginResponse>>> listar(
            @RequestParam(required = false) UUID idInstitucion,
            @RequestParam(required = false) String correo,
            @RequestParam(required = false, defaultValue = "false") boolean soloFallos,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false, defaultValue = "50") int limite) {

        // Determinar la institución efectiva
        UUID idInst = SecurityUtils.currentUserHasRole("SUPER_ADMIN")
                ? idInstitucion
                : SecurityUtils.requireCurrentInstitutionId();

        // Resolver rango de fechas
        Instant desde = fechaDesde != null ? fechaDesde.atStartOfDay().toInstant(ZoneOffset.UTC)
                : Instant.now().minusSeconds(86400L * 7); // última semana por defecto
        Instant hasta = fechaHasta != null ? fechaHasta.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
                : Instant.now();

        List<IntentoLogin> intentos = intentoRepo.buscarConFiltros(idInst, correo, soloFallos, desde, hasta,
                PageRequest.of(0, Math.min(limite, 200), Sort.by(Sort.Direction.DESC, "fechaIntento")));

        List<IntentoLoginResponse> resultado = intentos.stream()
                .map(IntentoLoginResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.ok("Intentos de login obtenidos", resultado));
    }
}
