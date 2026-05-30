package com.uagrm.si2g2.notificacion.application;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Servicio para enviar notificaciones push via Firebase Cloud Messaging (FCM).
 * <p>
 * Si el SDK no está inicializado (FIREBASE_CREDENTIALS_PATH no configurado),
 * los métodos registran un warning y retornan sin lanzar excepción, para que
 * el sistema funcione en entornos de desarrollo sin credenciales.
 */
@Slf4j
@Service
public class FcmService {

    /**
     * Envía una notificación a un dispositivo individual por su FCM token.
     *
     * @param fcmToken  token del dispositivo obtenido desde Flutter
     * @param titulo    título de la notificación
     * @param cuerpo    cuerpo/mensaje de la notificación
     * @param datos     datos adicionales como pares clave-valor (puede ser null)
     */
    public void enviarADispositivo(String fcmToken, String titulo, String cuerpo, Map<String, String> datos) {
        if (!isFirebaseDisponible()) return;

        try {
            Message.Builder builder = Message.builder()
                    .setNotification(Notification.builder()
                            .setTitle(titulo)
                            .setBody(cuerpo)
                            .build())
                    .setToken(fcmToken);

            if (datos != null && !datos.isEmpty()) {
                builder.putAllData(datos);
            }

            String messageId = FirebaseMessaging.getInstance().send(builder.build());
            log.debug("[FCM] Notificación enviada a token={}: messageId={}", fcmToken, messageId);
        } catch (FirebaseMessagingException e) {
            log.error("[FCM] Error enviando notificación a token={}: {}", fcmToken, e.getMessage());
        }
    }

    /**
     * Envía una notificación a múltiples dispositivos (hasta 500 tokens por llamada).
     *
     * @param fcmTokens lista de tokens FCM
     * @param titulo    título de la notificación
     * @param cuerpo    cuerpo/mensaje de la notificación
     * @param datos     datos adicionales (puede ser null)
     */
    public void enviarAMultiplesDispositivos(List<String> fcmTokens, String titulo, String cuerpo,
                                              Map<String, String> datos) {
        if (!isFirebaseDisponible() || fcmTokens == null || fcmTokens.isEmpty()) return;

        try {
            MulticastMessage.Builder builder = MulticastMessage.builder()
                    .setNotification(Notification.builder()
                            .setTitle(titulo)
                            .setBody(cuerpo)
                            .build())
                    .addAllTokens(fcmTokens);

            if (datos != null && !datos.isEmpty()) {
                builder.putAllData(datos);
            }

            var response = FirebaseMessaging.getInstance().sendEachForMulticast(builder.build());
            log.info("[FCM] Enviado a {} dispositivos — éxito: {}, fallido: {}",
                    fcmTokens.size(), response.getSuccessCount(), response.getFailureCount());
        } catch (FirebaseMessagingException e) {
            log.error("[FCM] Error enviando notificación multicast: {}", e.getMessage());
        }
    }

    /**
     * Envía una notificación a un topic de FCM (para grupos de dispositivos suscritos al topic).
     *
     * @param topic  nombre del topic (e.g. "institucion-{idInstitucion}")
     * @param titulo título de la notificación
     * @param cuerpo cuerpo/mensaje de la notificación
     * @param datos  datos adicionales (puede ser null)
     */
    public void enviarATopic(String topic, String titulo, String cuerpo, Map<String, String> datos) {
        if (!isFirebaseDisponible()) return;

        try {
            Message.Builder builder = Message.builder()
                    .setNotification(Notification.builder()
                            .setTitle(titulo)
                            .setBody(cuerpo)
                            .build())
                    .setTopic(topic);

            if (datos != null && !datos.isEmpty()) {
                builder.putAllData(datos);
            }

            String messageId = FirebaseMessaging.getInstance().send(builder.build());
            log.debug("[FCM] Notificación enviada al topic={}: messageId={}", topic, messageId);
        } catch (FirebaseMessagingException e) {
            log.error("[FCM] Error enviando notificación al topic={}: {}", topic, e.getMessage());
        }
    }

    private boolean isFirebaseDisponible() {
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("[FCM] Firebase no inicializado — notificación omitida");
            return false;
        }
        return true;
    }
}
