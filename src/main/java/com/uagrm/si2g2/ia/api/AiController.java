package com.uagrm.si2g2.ia.api;

import com.uagrm.si2g2.common.dto.ApiResponse;
import com.uagrm.si2g2.ia.application.AiIntegrationService;
import com.uagrm.si2g2.ia.dto.ConsultaNaturalIaRequest;
import com.uagrm.si2g2.ia.dto.ConsultaNaturalIaResponse;
import com.uagrm.si2g2.ia.dto.InterpretacionIaRequest;
import com.uagrm.si2g2.ia.dto.InterpretacionIaResponse;
import com.uagrm.si2g2.ia.dto.RiesgoEstudianteRequest;
import com.uagrm.si2g2.ia.dto.RiesgoEstudianteResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador puente Spring Boot → FastAPI IA.
 * Recibe las peticiones del frontend (JWT ya validado), las reenvía a FastAPI
 * con el mismo token (FastAPI valida el mismo secreto compartido).
 */
@RestController
@RequestMapping("/api/ia")
@RequiredArgsConstructor
public class AiController {

    private final AiIntegrationService aiService;

    /**
     * Predice el nivel de riesgo académico de una lista de estudiantes.
     * Roles permitidos: ADMIN_INSTITUCION, DIRECTOR, SECRETARIO, SUPER_ADMIN.
     */
    @PostMapping("/riesgo/predecir")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN_INSTITUCION','ROLE_DIRECTOR','ROLE_SECRETARIO','ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<RiesgoEstudianteResponse>>> predecirRiesgo(
            @Valid @RequestBody List<@Valid RiesgoEstudianteRequest> estudiantes,
            @RequestHeader("Authorization") String authHeader) {

        List<RiesgoEstudianteResponse> resultados = aiService.predecirRiesgo(estudiantes, authHeader);
        return ResponseEntity.ok(
                ApiResponse.ok("Predicción de riesgo académico completada", resultados));
    }

    /**
     * Interpreta una consulta en lenguaje natural y devuelve filtros estructurados
     * para usar en el motor de reportes.
     * Roles permitidos: ADMIN_INSTITUCION, DIRECTOR, SECRETARIO, SUPER_ADMIN.
     */
    @PostMapping("/reporte/interpretar")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN_INSTITUCION','ROLE_DIRECTOR','ROLE_SECRETARIO','ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<InterpretacionIaResponse>> interpretarConsulta(
            @Valid @RequestBody InterpretacionIaRequest request,
            @RequestHeader("Authorization") String authHeader) {

        InterpretacionIaResponse response = aiService.interpretarConsulta(request, authHeader);
        return ResponseEntity.ok(
                ApiResponse.ok("Consulta interpretada correctamente", response));
    }

    /**
     * Ejecuta una consulta en lenguaje natural: la IA genera SQL, lo ejecuta
     * contra la BD multi-tenant y retorna las filas resultantes.
     * Roles permitidos: ADMIN_INSTITUCION, DIRECTOR, SECRETARIO, SUPER_ADMIN.
     */
    @PostMapping("/reporte/consulta-natural")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN_INSTITUCION','ROLE_DIRECTOR','ROLE_SECRETARIO','ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ConsultaNaturalIaResponse>> consultaNatural(
            @Valid @RequestBody ConsultaNaturalIaRequest request,
            @RequestHeader("Authorization") String authHeader) {

        ConsultaNaturalIaResponse response = aiService.consultaNatural(request, authHeader);
        return ResponseEntity.ok(
                ApiResponse.ok("Consulta ejecutada correctamente", response));
    }
}
