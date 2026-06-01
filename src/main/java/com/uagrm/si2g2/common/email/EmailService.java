package com.uagrm.si2g2.common.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Envío de correos transaccionales (HTML inline, sin plantillas Thymeleaf).
 *
 * <p>Los fallos de envío se registran pero no propagan excepción, para no revertir
 * la transacción de negocio (p.ej. la generación del QR no debe fallar si el SMTP
 * no está configurado en entorno local/DEMO).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
  private final RestClient restClient = RestClient.builder().build();

  @Value("${app.mail.from:no-reply@example.com}")
    private String from;

  @Value("${app.mail.from-name:SIA}")
  private String fromName;

  @Value("${app.mail.transport:SMTP}")
  private String transport;

  @Value("${app.mail.mailtrap.api-url:https://send.api.mailtrap.io/api/send}")
  private String mailtrapApiUrl;

  @Value("${app.mail.mailtrap.token:}")
  private String mailtrapApiToken;

    /**
     * Envía el QR de pago al contacto de la solicitud, con la imagen embebida (inline CID).
     *
     * @param destinatario   correo del contacto
     * @param nombreContacto nombre del contacto
     * @param nombrePlan     nombre del plan contratado
     * @param monto          importe a pagar
     * @param moneda         moneda (p.ej. BOB)
     * @param qrBase64       imagen del QR en base64 (PNG, sin prefijo data:)
     */
    public void enviarQrPago(String destinatario, String nombreContacto, String nombrePlan,
                             BigDecimal monto, String moneda, String qrBase64) {
      String qrDataUri = buildQrDataUri(qrBase64);
      String qrBlock = qrDataUri != null
          ? "<img src=\"%s\" alt=\"QR de pago\" style=\"width:280px;height:280px;border:1px solid #e5e7eb;border-radius:8px\" />".formatted(qrDataUri)
          : "<div style=\"padding:10px 14px;border:1px dashed #9ca3af;border-radius:8px;color:#6b7280\">QR no disponible</div>";

        String html = """
                <div style="font-family:Arial,Helvetica,sans-serif;max-width:560px;margin:0 auto;color:#1f2937">
                  <h2 style="color:#111827">Pago de tu suscripción</h2>
                  <p>Hola <strong>%s</strong>,</p>
                  <p>Tu solicitud fue aprobada. Para activar tu institución, completa el pago del plan
                     <strong>%s</strong> escaneando el siguiente código QR desde tu app de banca móvil.</p>
                  <div style="text-align:center;margin:24px 0">
            %s
                  </div>
                  <p style="text-align:center;font-size:20px;font-weight:bold;color:#059669">%s %s</p>
                  <p style="font-size:13px;color:#6b7280">Una vez confirmado el pago, tu institución será activada y
                     recibirás las credenciales de acceso.</p>
                  <hr style="border:none;border-top:1px solid #e5e7eb;margin:24px 0" />
                  <p style="font-size:12px;color:#9ca3af">SIA — Sistema de Gestión Académica</p>
                </div>
                """.formatted(
              safe(nombreContacto), safe(nombrePlan), qrBlock,
                        moneda, monto != null ? monto.toPlainString() : "0.00");
      enviar(destinatario, "Pago de tu suscripción — " + nombrePlan, html, "QR de pago");
    }

    /**
     * Envía la confirmación de pago al contacto de la solicitud.
     */
    public void enviarConfirmacionPago(String destinatario, String nombreContacto,
                                       String nombrePlan, BigDecimal monto, String moneda) {
        String html = """
                <div style="font-family:Arial,Helvetica,sans-serif;max-width:560px;margin:0 auto;color:#1f2937">
                  <h2 style="color:#059669">¡Pago confirmado!</h2>
                  <p>Hola <strong>%s</strong>,</p>
                  <p>Hemos recibido tu pago del plan <strong>%s</strong> por
                     <strong>%s %s</strong>.</p>
                  <p>Tu institución será activada en breve y recibirás las credenciales de acceso
                     del administrador.</p>
                  <hr style="border:none;border-top:1px solid #e5e7eb;margin:24px 0" />
                  <p style="font-size:12px;color:#9ca3af">SIA — Sistema de Gestión Académica</p>
                </div>
                """.formatted(
                        safe(nombreContacto), safe(nombrePlan),
                        moneda, monto != null ? monto.toPlainString() : "0.00");

        enviar(destinatario, "Pago confirmado — " + nombrePlan, html, "confirmación de pago");
    }

    /**
     * Notifica al contacto que su solicitud fue aprobada e incluye el link de pago.
     */
    public void enviarAprobacionConLinkPago(String destinatario, String nombreContacto,
                                             String nombrePlan, BigDecimal monto, String moneda,
                                             String linkPago) {
        String html = """
                <div style="font-family:Arial,Helvetica,sans-serif;max-width:560px;margin:0 auto;color:#1f2937">
                  <h2 style="color:#2563eb">¡Tu solicitud fue aprobada!</h2>
                  <p>Hola <strong>%s</strong>,</p>
                  <p>Nos complace informarte que tu solicitud para el plan <strong>%s</strong> ha sido
                     <strong>aprobada</strong>.</p>
                  <p>Para activar tu institución, completa el pago de <strong>%s %s</strong> haciendo
                     clic en el siguiente botón:</p>
                  <div style="text-align:center;margin:32px 0">
                    <a href="%s"
                       style="background:#2563eb;color:#fff;text-decoration:none;padding:14px 32px;
                              border-radius:8px;font-size:16px;font-weight:bold;display:inline-block">
                      Pagar ahora
                    </a>
                  </div>
                  <p style="font-size:13px;color:#6b7280">
                    O copia este enlace en tu navegador:<br/>
                    <a href="%s" style="color:#2563eb">%s</a>
                  </p>
                  <p style="font-size:13px;color:#6b7280">
                    El enlace estará disponible durante los próximos 14 días.
                    Podrás pagar con el código QR que se generará al abrir la página.
                  </p>
                  <hr style="border:none;border-top:1px solid #e5e7eb;margin:24px 0" />
                  <p style="font-size:12px;color:#9ca3af">SIA — Sistema de Gestión Académica</p>
                </div>
                """.formatted(
                        safe(nombreContacto), safe(nombrePlan),
                        moneda, monto != null ? monto.toPlainString() : "0.00",
                        safe(linkPago), safe(linkPago), safe(linkPago));

        enviar(destinatario, "Solicitud aprobada — activa tu institución", html, "aprobación");
    }

    /**
     * Envía el correo de bienvenida tras la activación automática de la institución.
     * Incluye el link para que el admin cree su contraseña por primera vez.
     */
    public void enviarBienvenidaConActivacion(String destinatario, String nombreContacto,
                                               String nombrePlan, String correoLogin,
                                               String linkCrearContrasena) {
        String html = """
                <div style="font-family:Arial,Helvetica,sans-serif;max-width:560px;margin:0 auto;color:#1f2937">
                  <h2 style="color:#059669">¡Tu institución está activa!</h2>
                  <p>Hola <strong>%s</strong>,</p>
                  <p>Tu pago fue confirmado y tu institución ha sido activada con el plan
                     <strong>%s</strong>. ¡Ya puedes empezar a usar el sistema!</p>
                  <div style="background:#f0fdf4;border:1px solid #86efac;border-radius:8px;padding:16px 20px;margin:20px 0">
                    <p style="margin:0 0 8px 0"><strong>Tus datos de acceso:</strong></p>
                    <p style="margin:0">Correo: <strong>%s</strong></p>
                    <p style="margin:4px 0 0 0">Rol: <strong>Administrador de Institución</strong></p>
                  </div>
                  <p>Para ingresar, primero debes crear tu contraseña haciendo clic aquí:</p>
                  <div style="text-align:center;margin:32px 0">
                    <a href="%s"
                       style="background:#059669;color:#fff;text-decoration:none;padding:14px 32px;
                              border-radius:8px;font-size:16px;font-weight:bold;display:inline-block">
                      Crear mi contraseña
                    </a>
                  </div>
                  <p style="font-size:13px;color:#6b7280">
                    O copia este enlace en tu navegador:<br/>
                    <a href="%s" style="color:#059669">%s</a>
                  </p>
                  <p style="font-size:12px;color:#ef4444">
                    Este enlace expira en 7 días. Si no lo usas, puedes solicitar uno nuevo
                    desde la página de inicio de sesión usando la opción "¿Olvidaste tu contraseña?".
                  </p>
                  <hr style="border:none;border-top:1px solid #e5e7eb;margin:24px 0" />
                  <p style="font-size:12px;color:#9ca3af">SIA — Sistema de Gestión Académica</p>
                </div>
                """.formatted(
                        safe(nombreContacto), safe(nombrePlan),
                        safe(correoLogin),
                        safe(linkCrearContrasena), safe(linkCrearContrasena), safe(linkCrearContrasena));

        enviar(destinatario, "¡Bienvenido a SIA! Crea tu contraseña", html, "bienvenida");
    }

      /**
       * Envía un correo de prueba para validar configuración SMTP y entrega.
       */
      public void enviarCorreoPrueba(String destinatario, String asunto, String mensajePlano) {
        String html = """
            <div style="font-family:Arial,Helvetica,sans-serif;max-width:560px;margin:0 auto;color:#1f2937">
              <h2 style="color:#2563eb">Correo de prueba</h2>
              <p>Este mensaje fue enviado desde el backend de SIA para verificar la configuración de correo.</p>
              <div style="background:#f9fafb;border:1px solid #e5e7eb;border-radius:8px;padding:16px 20px;margin:20px 0;white-space:pre-wrap">
              %s
              </div>
              <hr style="border:none;border-top:1px solid #e5e7eb;margin:24px 0" />
              <p style="font-size:12px;color:#9ca3af">SIA — Sistema de Gestión Académica</p>
            </div>
            """.formatted(safe(mensajePlano));

        enviar(destinatario, asunto, html, "correo de prueba");
      }

      private void enviar(String destinatario, String asunto, String html, String contexto) {
        if (isMailtrapApiTransport()) {
          enviarPorMailtrapApi(destinatario, asunto, html, contexto);
          return;
        }

        enviarPorSmtp(destinatario, asunto, html, contexto);
      }

      private boolean isMailtrapApiTransport() {
        return "MAILTRAP_API".equalsIgnoreCase(transport);
      }

      private void enviarPorSmtp(String destinatario, String asunto, String html, String contexto) {
        try {
          MimeMessage message = mailSender.createMimeMessage();
          MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
          helper.setFrom(from);
          helper.setTo(destinatario);
          helper.setSubject(asunto);
          helper.setText(html, true);
          mailSender.send(message);
          log.info("[MAIL] {} enviado por SMTP a {}", contexto, destinatario);
        } catch (MessagingException | RuntimeException ex) {
          log.warn("[MAIL] No se pudo enviar {} por SMTP a {}: {}", contexto, destinatario, ex.getMessage());
        }
    }

      private void enviarPorMailtrapApi(String destinatario, String asunto, String html, String contexto) {
        if (mailtrapApiToken == null || mailtrapApiToken.isBlank()) {
          log.warn("[MAIL] Transporte MAILTRAP_API activo, pero falta app.mail.mailtrap.token. No se envió {}.", contexto);
          return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("from", Map.of("email", from, "name", fromName));
        payload.put("to", List.of(Map.of("email", destinatario)));
        payload.put("subject", asunto);
        payload.put("html", html);
        payload.put("category", "SIA Transactional");

        try {
          restClient.post()
              .uri(mailtrapApiUrl)
              .contentType(MediaType.APPLICATION_JSON)
              .header("Authorization", "Bearer " + mailtrapApiToken)
              .body(payload)
              .retrieve()
              .toBodilessEntity();

          log.info("[MAIL] {} enviado por Mailtrap API a {}", contexto, destinatario);
        } catch (RuntimeException ex) {
          log.warn("[MAIL] No se pudo enviar {} por Mailtrap API a {}: {}", contexto, destinatario, ex.getMessage());
        }
      }

      private String buildQrDataUri(String qrBase64) {
        if (qrBase64 == null || qrBase64.isBlank()) {
          return null;
        }
        String clean = qrBase64.contains(",") ? qrBase64.substring(qrBase64.indexOf(',') + 1) : qrBase64;
        return "data:image/png;base64," + clean.trim();
      }

    private String safe(String value) {
        return value != null ? value : "";
    }
}
