package com.uagrm.si2g2.auditoria.api;

import com.uagrm.si2g2.auditoria.application.AuditoriaQueryService;
import com.uagrm.si2g2.auditoria.dto.BitacoraAuditoriaFiltro;
import com.uagrm.si2g2.auditoria.dto.BitacoraAuditoriaResponse;
import com.uagrm.si2g2.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auditoria")
@RequiredArgsConstructor
public class AuditoriaController {

    private final AuditoriaQueryService queryService;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('AUDITORIA_READ')")
    public ResponseEntity<ApiResponse<List<BitacoraAuditoriaResponse>>> listar(
            @RequestParam(required = false) String modulo,
            @RequestParam(required = false) String tipoOperacion,
            @RequestParam(required = false) Boolean exito) {
        BitacoraAuditoriaFiltro filtro = new BitacoraAuditoriaFiltro();
        filtro.setModulo(modulo);
        filtro.setTipoOperacion(tipoOperacion);
        filtro.setExito(exito);
        return ResponseEntity.ok(ApiResponse.ok("Bitácora de auditoría", queryService.listar(filtro)));
    }
}
