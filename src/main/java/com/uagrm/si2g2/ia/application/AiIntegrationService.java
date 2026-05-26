package com.uagrm.si2g2.ia.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uagrm.si2g2.common.dto.ApiResponse;
import com.uagrm.si2g2.ia.dto.FiltroReporteDto;
import com.uagrm.si2g2.ia.dto.InterpretacionIaRequest;
import com.uagrm.si2g2.ia.dto.InterpretacionIaResponse;
import com.uagrm.si2g2.ia.dto.RiesgoEstudianteRequest;
import com.uagrm.si2g2.ia.dto.RiesgoEstudianteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiIntegrationService {

    private final ObjectMapper objectMapper;

    @Value("${app.fastapi.base-url:http://localhost:8001}")
    private String fastapiBaseUrl;

    // ── RestClient (creado bajo demanda; lazy para que base-url esté inyectado) ──

    private volatile RestClient restClient;

    private RestClient client() {
        if (restClient == null) {
            synchronized (this) {
                if (restClient == null) {
                    restClient = RestClient.builder()
                            .baseUrl(fastapiBaseUrl)
                            .build();
                }
            }
        }
        return restClient;
    }

    // ── Riesgo académico ───────────────────────────────────────────────────────

    /**
     * Llama a FastAPI POST /api/ia/riesgo/predecir.
     * Reenvía el JWT del usuario autenticado (mismo secreto compartido).
     *
     * @param estudiantes lista de inputs por estudiante
     * @param authHeader  valor del header "Authorization: Bearer <token>"
     */
    public List<RiesgoEstudianteResponse> predecirRiesgo(
            List<RiesgoEstudianteRequest> estudiantes,
            String authHeader) {

        log.info("[IA] predecirRiesgo — {} estudiantes → {}", estudiantes.size(), fastapiBaseUrl);
        long start = System.currentTimeMillis();

        try {
            Map<?, ?> body = client().post()
                    .uri("/api/ia/riesgo/predecir")
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(estudiantes)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new RuntimeException("FastAPI error " + res.getStatusCode());
                    })
                    .body(Map.class);

            List<Object> data = extractDataAsList(body);
            List<RiesgoEstudianteResponse> resultados =
                    objectMapper.convertValue(data, new TypeReference<>() {});

            log.info("[IA] predecirRiesgo OK — {} ms", System.currentTimeMillis() - start);
            return resultados;

        } catch (RestClientResponseException ex) {
            log.error("[IA] predecirRiesgo FAILED — {}", ex.getMessage());
            throw new RuntimeException("Error al consultar IA para predicción de riesgo: " + ex.getMessage(), ex);
        }
    }

    // ── Interpretación de lenguaje natural ────────────────────────────────────

    /**
     * Llama a FastAPI POST /api/ia/reporte/interpretar.
     *
     * @param request    texto en lenguaje natural + entidad objetivo
     * @param authHeader JWT del usuario ("Authorization: Bearer <token>")
     */
    public InterpretacionIaResponse interpretarConsulta(
            InterpretacionIaRequest request,
            String authHeader) {

        log.info("[IA] interpretarConsulta — entidad='{}' → {}", request.entidad(), fastapiBaseUrl);
        long start = System.currentTimeMillis();

        try {
            Map<?, ?> body = client().post()
                    .uri("/api/ia/reporte/interpretar")
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new RuntimeException("FastAPI error " + res.getStatusCode());
                    })
                    .body(Map.class);

            Map<String, Object> data = extractDataAsMap(body);
            InterpretacionIaResponse response = objectMapper.convertValue(data, InterpretacionIaResponse.class);

            log.info("[IA] interpretarConsulta OK — {} ms", System.currentTimeMillis() - start);
            return response;

        } catch (RestClientResponseException ex) {
            log.error("[IA] interpretarConsulta FAILED — {}", ex.getMessage());
            throw new RuntimeException("Error al interpretar consulta con IA: " + ex.getMessage(), ex);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<Object> extractDataAsList(Map<?, ?> apiResponse) {
        Object data = apiResponse != null ? apiResponse.get("data") : null;
        if (data instanceof List<?> list) {
            return (List<Object>) list;
        }
        throw new RuntimeException("Respuesta inesperada de FastAPI: campo 'data' no es una lista");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractDataAsMap(Map<?, ?> apiResponse) {
        Object data = apiResponse != null ? apiResponse.get("data") : null;
        if (data instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new RuntimeException("Respuesta inesperada de FastAPI: campo 'data' no es un objeto");
    }
}
