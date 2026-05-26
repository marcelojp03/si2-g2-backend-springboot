package com.uagrm.si2g2.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${app.firebase.credentials-path:gestion-academica-firebase.json}")
    private String credentialsPath;

    @PostConstruct
    public void initialize() {
        if (FirebaseApp.getApps().isEmpty()) {
            try (InputStream serviceAccount = resolveCredentials()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();
                FirebaseApp.initializeApp(options);
                log.info("[Firebase] SDK inicializado correctamente (proyecto: gestion-academica-3e2e9)");
            } catch (IOException e) {
                log.warn("[Firebase] No se pudo inicializar el SDK — notificaciones push deshabilitadas. " +
                        "Verifica FIREBASE_CREDENTIALS_PATH. Detalle: {}", e.getMessage());
            }
        }
    }

    /**
     * Busca el archivo de credenciales primero como ruta de sistema de archivos,
     * luego como recurso en el classpath.
     */
    private InputStream resolveCredentials() throws IOException {
        if (!StringUtils.hasText(credentialsPath)) {
            throw new IOException("FIREBASE_CREDENTIALS_PATH no está configurado");
        }

        // 1. Ruta absoluta o relativa al directorio de trabajo
        Path filePath = Paths.get(credentialsPath);
        if (Files.exists(filePath)) {
            return new FileInputStream(filePath.toFile());
        }

        // 2. Classpath (si está en src/main/resources/)
        try {
            return new ClassPathResource(credentialsPath).getInputStream();
        } catch (IOException ignored) {
            // no está en classpath
        }

        throw new IOException("No se encontró el archivo de credenciales Firebase en: " + credentialsPath);
    }
}
