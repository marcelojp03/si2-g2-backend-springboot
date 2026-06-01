package com.uagrm.si2g2.saas.pago.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente de la pasarela de pago Vpay.
 *
 * <p>Soporta dos modos:</p>
 * <ul>
 *   <li><b>Producción</b>: cuando {@code app.vpay.token} está configurado, genera un QR real
 *       (operación VTO041, {@code PUT api/transactions/doPayment}) y consulta su estado
 *       ({@code POST api/operations/statusQr}).</li>
 *   <li><b>DEMO</b>: cuando el token está vacío, genera un QR de demostración con
 *       {@code api.qrserver.com} y el estado siempre devuelve "PEN" (el pago se confirma
 *       manualmente desde el panel SUPER_ADMIN).</li>
 * </ul>
 */
@Slf4j
@Component
public class VpayClient {

    private static final String DEMO_QR_PROVIDER = "https://api.qrserver.com/v1/create-qr-code/";

    @Value("${app.vpay.base-url:https://vpay.com.bo:7778/pro}")
    private String baseUrl;

    @Value("${app.vpay.token:}")
    private String token;

    @Value("${app.vpay.user:}")
    private String user;

    @Value("${app.vpay.company:1}")
    private String company;

    @Value("${app.vpay.bank:BMSC}")
    private String bank;

    @Value("${app.vpay.destination-account:}")
    private String destinationAccount;

    @Value("${app.vpay.verify-ssl:true}")
    private boolean verifySsl;

    private final ObjectMapper objectMapper;

    private volatile RestClient vpayClient;
    private volatile RestClient demoClient;

    public VpayClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** {@code true} si no hay token configurado: se opera en modo demostración. */
    public boolean isDemo() {
        return token == null || token.isBlank();
    }

    // ── Generación de QR ────────────────────────────────────────────────────────

    /**
     * Genera un QR de cobro.
     *
     * @param monto          importe a cobrar
     * @param glosa          descripción del cobro
     * @param expiracion     fecha de expiración del QR
     * @param additionalData dato adicional para conciliación (p.ej. id de solicitud)
     * @return id del QR + imagen en base64
     */
    public QrResult generarQr(java.math.BigDecimal monto, String glosa,
                              LocalDate expiracion, String additionalData) {
        if (isDemo()) {
            return generarQrDemo(monto, glosa, additionalData);
        }
        return generarQrProduccion(monto, glosa, expiracion, additionalData);
    }

    private QrResult generarQrDemo(java.math.BigDecimal monto, String glosa, String additionalData) {
        String contenido = "VPAY-DEMO|monto=" + monto + "|glosa=" + glosa + "|ref=" + additionalData;
        String url = DEMO_QR_PROVIDER + "?size=320x320&data="
                + URLEncoder.encode(contenido, StandardCharsets.UTF_8);
        try {
            byte[] png = demoClient().get().uri(url).retrieve().body(byte[].class);
            String base64 = png != null ? Base64.getEncoder().encodeToString(png) : "";
            String idQr = "DEMO-" + System.currentTimeMillis();
            log.info("[VPAY][DEMO] QR generado idQr={} monto={}", idQr, monto);
            return new QrResult(idQr, base64);
        } catch (Exception ex) {
            log.warn("[VPAY][DEMO] No se pudo obtener QR de demostración: {}", ex.getMessage());
            return new QrResult("DEMO-" + System.currentTimeMillis(), "");
        }
    }

    private QrResult generarQrProduccion(java.math.BigDecimal monto, String glosa,
                                         LocalDate expiracion, String additionalData) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("operation", "VTO041");
        body.put("user", user);
        body.put("company", company);
        body.put("header", List.of(
                attr("amount", monto.toPlainString()),
                attr("currency", "BOB"),
                attr("gloss", glosa),
                attr("expiration", expiracion != null ? expiracion.toString() : ""),
                attr("account", destinationAccount),
                attr("bank", bank),
                attr("additionalData", additionalData != null ? additionalData : "")
        ));

        try {
            String raw = vpayClient().put()
                    .uri("/api/transactions/doPayment")
                    .header("Authorization", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(raw);
            JsonNode response = firstResponseList(root);

            String idQr = findIdentificatorByCodeHint(response, "id", "qr");
            String qrBase64 = findIdentificatorByCodeHint(response, "image", "qr", "base64");

            if (idQr == null || idQr.isBlank()) {
                throw new IllegalStateException("Vpay no devolvió el id del QR. Respuesta: " + raw);
            }
            log.info("[VPAY] QR generado idQr={} monto={}", idQr, monto);
            return new QrResult(idQr, qrBase64 != null ? qrBase64 : "");
        } catch (Exception ex) {
            log.error("[VPAY] Error generando QR: {}", ex.getMessage());
            throw new IllegalStateException("No se pudo generar el QR de pago con Vpay: " + ex.getMessage(), ex);
        }
    }

    // ── Consulta de estado ──────────────────────────────────────────────────────

    /**
     * Consulta el estado del QR.
     *
     * @param idQr identificador del QR
     * @return "PAG" (pagado) o "PEN" (pendiente)
     */
    public String consultarEstadoQr(String idQr) {
        if (isDemo() || idQr == null || idQr.startsWith("DEMO-")) {
            return "PEN";
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("operation", idQr);
        body.put("user", user);
        body.put("company", company);

        try {
            String raw = vpayClient().post()
                    .uri("/api/operations/statusQr")
                    .header("Authorization", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode response = firstResponseList(objectMapper.readTree(raw));
            String estado = extraerEstado(response);
            log.info("[VPAY] Estado QR idQr={} estado={}", idQr, estado);
            return estado;
        } catch (Exception ex) {
            log.warn("[VPAY] Error consultando estado del QR {}: {}", idQr, ex.getMessage());
            return "PEN";
        }
    }

    // ── Helpers de parseo ───────────────────────────────────────────────────────

    private Map<String, Object> attr(String code, String value) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", code);
        m.put("value", value);
        return m;
    }

    private JsonNode firstResponseList(JsonNode root) {
        JsonNode list = root.path("responseList");
        if (list.isArray() && !list.isEmpty()) {
            return list.get(0).path("response");
        }
        return objectMapper.createArrayNode();
    }

    /** Busca el "identificator" cuyo "code" contenga alguna de las pistas (case-insensitive). */
    private String findIdentificatorByCodeHint(JsonNode response, String... hints) {
        if (response == null || !response.isArray()) return null;
        for (JsonNode item : response) {
            String code = item.path("code").asText("").toLowerCase();
            for (String hint : hints) {
                if (code.contains(hint.toLowerCase())) {
                    return item.path("identificator").asText(null);
                }
            }
        }
        return null;
    }

    /** Determina el estado del QR ("PAG"/"PEN") a partir de los identificadores devueltos. */
    private String extraerEstado(JsonNode response) {
        if (response != null && response.isArray()) {
            for (JsonNode item : response) {
                String value = item.path("identificator").asText("").toUpperCase();
                if (value.contains("PAG")) return "PAG";
            }
            for (JsonNode item : response) {
                String value = item.path("identificator").asText("").toUpperCase();
                if (value.contains("PEN")) return "PEN";
            }
        }
        return "PEN";
    }

    // ── RestClients (lazy) ──────────────────────────────────────────────────────

    private RestClient vpayClient() {
        if (vpayClient == null) {
            synchronized (this) {
                if (vpayClient == null) {
                    RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl);
                    if (!verifySsl) {
                        builder.requestFactory(new JdkClientHttpRequestFactory(trustAllHttpClient()));
                    }
                    vpayClient = builder.build();
                }
            }
        }
        return vpayClient;
    }

    private RestClient demoClient() {
        if (demoClient == null) {
            synchronized (this) {
                if (demoClient == null) {
                    demoClient = RestClient.builder().build();
                }
            }
        }
        return demoClient;
    }

    private HttpClient trustAllHttpClient() {
        try {
            TrustManager[] trustAll = new TrustManager[]{ new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                public void checkClientTrusted(X509Certificate[] chain, String authType) { }
                public void checkServerTrusted(X509Certificate[] chain, String authType) { }
            }};
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAll, new SecureRandom());
            return HttpClient.newBuilder().sslContext(sslContext).build();
        } catch (Exception ex) {
            log.warn("[VPAY] No se pudo desactivar la verificación SSL: {}", ex.getMessage());
            return HttpClient.newHttpClient();
        }
    }
}
