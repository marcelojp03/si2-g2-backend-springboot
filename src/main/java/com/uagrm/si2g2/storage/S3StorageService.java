package com.uagrm.si2g2.storage;

import com.uagrm.si2g2.storage.dto.ArchivoSubidoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class S3StorageService {

    private static final long MAX_BYTES = 10L * 1024 * 1024; // 10 MB

    // Extensiones permitidas → content-type
    private static final Map<String, String> TIPOS_PERMITIDOS = Map.of(
            "jpg",  "image/jpeg",
            "jpeg", "image/jpeg",
            "png",  "image/png",
            "webp", "image/webp",
            "gif",  "image/gif",
            "pdf",  "application/pdf"
    );

    private static final Set<String> CARPETAS_VALIDAS = Set.of(
            "logos", "estudiantes", "docentes", "examenes", "archivos"
    );

    @Value("${app.aws.s3.bucket}")
    private String bucket;

    @Value("${app.aws.s3.region}")
    private String region;

    private final S3Client s3;

    public S3StorageService(@Value("${app.aws.s3.region}") String region) {
        this.region = region;
        this.s3 = S3Client.builder()
                .region(Region.of(region))
                .build();
    }

    public ArchivoSubidoResponse subir(MultipartFile archivo, String idInstitucion, String carpeta) {
        validarCarpeta(carpeta);
        validarArchivo(archivo);

        String extension = obtenerExtension(archivo.getOriginalFilename());
        String contentType = TIPOS_PERMITIDOS.get(extension.toLowerCase());
        String clave = carpeta + "/" + idInstitucion + "/" + UUID.randomUUID() + "." + extension;

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(clave)
                    .contentType(contentType)
                    .contentLength(archivo.getSize())
                    .build();

            s3.putObject(request, RequestBody.fromBytes(archivo.getBytes()));

            String url = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + clave;
            log.info("Archivo subido a S3: {}", clave);

            return ArchivoSubidoResponse.builder()
                    .url(url)
                    .nombre(archivo.getOriginalFilename())
                    .tipo(contentType)
                    .tamanioBytes(archivo.getSize())
                    .build();

        } catch (IOException e) {
            throw new IllegalStateException("Error al leer el archivo: " + e.getMessage());
        }
    }

    private void validarCarpeta(String carpeta) {
        if (carpeta == null || !CARPETAS_VALIDAS.contains(carpeta.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Carpeta no válida. Debe ser una de: " + String.join(", ", CARPETAS_VALIDAS));
        }
    }

    private void validarArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo no puede estar vacío");
        }
        if (archivo.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("El archivo supera el tamaño máximo de 10 MB");
        }
        String extension = obtenerExtension(archivo.getOriginalFilename());
        if (!TIPOS_PERMITIDOS.containsKey(extension.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Tipo de archivo no permitido. Extensiones válidas: "
                    + String.join(", ", TIPOS_PERMITIDOS.keySet()));
        }
    }

    private String obtenerExtension(String nombreArchivo) {
        if (nombreArchivo == null || !nombreArchivo.contains(".")) {
            throw new IllegalArgumentException("El archivo debe tener una extensión válida");
        }
        return nombreArchivo.substring(nombreArchivo.lastIndexOf('.') + 1);
    }
}
