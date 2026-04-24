package com.uagrm.si2g2.storage;

import com.uagrm.si2g2.storage.domain.Archivo;
import com.uagrm.si2g2.storage.domain.ArchivoReferencia;
import com.uagrm.si2g2.storage.domain.ArchivoReferenciaRepository;
import com.uagrm.si2g2.storage.domain.ArchivoRepository;
import com.uagrm.si2g2.storage.dto.ArchivoResponse;
import com.uagrm.si2g2.storage.dto.ArchivoUploadRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArchivoService {

    private static final long MAX_BYTES = 10L * 1024 * 1024;

    private static final Map<String, String> TIPOS_PERMITIDOS = Map.of(
            "jpg",  "image/jpeg",
            "jpeg", "image/jpeg",
            "png",  "image/png",
            "webp", "image/webp",
            "gif",  "image/gif",
            "pdf",  "application/pdf"
    );

    private static final Set<String> EXTENSIONES_IMAGEN = Set.of("jpg", "jpeg", "png", "webp", "gif");

    @Value("${app.aws.s3.bucket}")
    private String bucket;

    @Value("${app.aws.s3.region}")
    private String region;

    @Value("${app.aws.s3.presigned-url-expiration-minutes:60}")
    private int presignedUrlExpirationMinutes;

    private final S3Client s3;
    private final S3Presigner s3Presigner;
    private final ArchivoRepository archivoRepository;
    private final ArchivoReferenciaRepository archivoReferenciaRepository;

    /**
     * Sube un archivo a S3, lo registra en BD y crea la referencia con la entidad indicada.
     */
    @Transactional
    public ArchivoResponse subirYRegistrar(MultipartFile file, UUID idInstitucion,
                                           UUID idUsuario, ArchivoUploadRequest req) {
        validarArchivo(file);

        String ext = obtenerExtension(file.getOriginalFilename());
        String contentType = TIPOS_PERMITIDOS.get(ext.toLowerCase());
        String nombreArchivo = UUID.randomUUID() + "." + ext;
        String key = buildKey(req.getModulo(), idInstitucion, nombreArchivo);
        String categoria = EXTENSIONES_IMAGEN.contains(ext.toLowerCase()) ? "IMAGEN" : "DOCUMENTO";

        // 1. Subir a S3
        String etag = subirS3(file, key, contentType);

        // 2. Registrar metadatos en BD
        Archivo archivo = Archivo.builder()
                .idInstitucion(idInstitucion)
                .idUsuarioSubio(idUsuario)
                .nombreOriginal(file.getOriginalFilename())
                .nombreArchivo(nombreArchivo)
                .extension(ext)
                .mimeType(contentType)
                .tamanoBytes(file.getSize())
                .bucketS3(bucket)
                .regionS3(region)
                .keyS3(key)
                .etag(etag)
                .categoria(categoria)
                .visibilidad("PUBLICO")
                .estado("ACTIVO")
                .build();
        archivoRepository.save(archivo);

        // 3. Si esPrincipal, desactivar el principal anterior del mismo tipo/entidad
        if (Boolean.TRUE.equals(req.getEsPrincipal())) {
            archivoReferenciaRepository.desactivarPrincipalActual(
                    idInstitucion, req.getModulo(), req.getEntidad(),
                    req.getIdEntidad(), req.getTipoReferencia());
        }

        // 4. Crear referencia
        ArchivoReferencia ref = ArchivoReferencia.builder()
                .idInstitucion(idInstitucion)
                .idArchivo(archivo.getId())
                .modulo(req.getModulo())
                .entidad(req.getEntidad())
                .idEntidad(req.getIdEntidad())
                .tipoReferencia(req.getTipoReferencia())
                .esPrincipal(Boolean.TRUE.equals(req.getEsPrincipal()))
                .observacion(req.getObservacion())
                .estado("ACTIVO")
                .build();
        archivoReferenciaRepository.save(ref);

        log.info("Archivo registrado: id={} key={}", archivo.getId(), key);

        return toResponse(archivo, ref);
    }

    /**
     * Lista todos los archivos activos de una entidad.
     */
    @Transactional(readOnly = true)
    public List<ArchivoResponse> listarPorEntidad(UUID idInstitucion, String modulo,
                                                   String entidad, UUID idEntidad) {
        return archivoReferenciaRepository
                .findByIdInstitucionAndModuloAndEntidadAndIdEntidadAndEstado(
                        idInstitucion, modulo, entidad, idEntidad, "ACTIVO")
                .stream()
                .map(ref -> archivoRepository.findById(ref.getIdArchivo())
                        .filter(a -> "ACTIVO".equals(a.getEstado()))
                        .map(a -> toResponse(a, ref))
                        .orElse(null))
                .filter(r -> r != null)
                .toList();
    }

    /**
     * Obtiene el archivo principal activo de un tipo para una entidad.
     */
    @Transactional(readOnly = true)
    public Optional<ArchivoResponse> obtenerPrincipal(UUID idInstitucion, String modulo,
                                                       String entidad, UUID idEntidad,
                                                       String tipoReferencia) {
        return archivoReferenciaRepository
                .findByIdInstitucionAndModuloAndEntidadAndIdEntidadAndTipoReferenciaAndEsPrincipalTrueAndEstado(
                        idInstitucion, modulo, entidad, idEntidad, tipoReferencia, "ACTIVO")
                .flatMap(ref -> archivoRepository.findById(ref.getIdArchivo())
                        .filter(a -> "ACTIVO".equals(a.getEstado()))
                        .map(a -> toResponse(a, ref)));
    }

    /**
     * Eliminación lógica: marca archivo y referencia como eliminados/inactivos.
     */
    @Transactional
    public void eliminar(UUID idArchivo, UUID idInstitucion) {
        Archivo archivo = archivoRepository.findById(idArchivo)
                .orElseThrow(() -> new IllegalArgumentException("Archivo no encontrado"));
        if (!archivo.getIdInstitucion().equals(idInstitucion)) {
            throw new IllegalArgumentException("No tiene permiso para eliminar este archivo");
        }
        archivo.setEstado("ELIMINADO");
        archivoRepository.save(archivo);
        log.info("Archivo eliminado lógicamente: id={}", idArchivo);
    }

    // -----------------------------------------------------------------------

    private String subirS3(MultipartFile file, String key, String contentType) {
        try {
            PutObjectRequest req = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .contentLength(file.getSize())
                    .build();
            PutObjectResponse resp = s3.putObject(req, RequestBody.fromBytes(file.getBytes()));
            return resp.eTag();
        } catch (IOException e) {
            throw new IllegalStateException("Error al leer el archivo para subir a S3: " + e.getMessage());
        }
    }

    private String buildKey(String modulo, UUID idInstitucion, String nombreArchivo) {
        String carpeta = switch (modulo.toUpperCase()) {
            case "INSTITUCION" -> "logos";
            case "ESTUDIANTE"  -> "estudiantes";
            case "DOCENTE"     -> "docentes";
            case "TUTOR"       -> "tutores";
            case "EVALUACION"  -> "examenes";
            default            -> "archivos";
        };
        return carpeta + "/" + idInstitucion + "/" + nombreArchivo;
    }

    private String generarUrlFirmada(String key) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(presignedUrlExpirationMinutes))
                    .getObjectRequest(getObjectRequest)
                    .build();
            PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
            return presigned.url().toString();
        } catch (Exception e) {
            log.error("Error generando URL firmada para key={}: {}", key, e.getMessage());
            return null;
        }
    }

    private ArchivoResponse toResponse(Archivo a, ArchivoReferencia ref) {
        return ArchivoResponse.builder()
                .id(a.getId())
                .nombreOriginal(a.getNombreOriginal())
                .mimeType(a.getMimeType())
                .tamanoBytes(a.getTamanoBytes())
                .extension(a.getExtension())
                .categoria(a.getCategoria())
                .visibilidad(a.getVisibilidad())
                .url(generarUrlFirmada(a.getKeyS3()))
                .modulo(ref.getModulo())
                .entidad(ref.getEntidad())
                .idEntidad(ref.getIdEntidad())
                .tipoReferencia(ref.getTipoReferencia())
                .esPrincipal(ref.getEsPrincipal())
                .creadoEn(a.getCreadoEn())
                .build();
    }

    private void validarArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo no puede estar vacío");
        }
        if (archivo.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("El archivo supera el tamaño máximo de 10 MB");
        }
        String ext = obtenerExtension(archivo.getOriginalFilename());
        if (!TIPOS_PERMITIDOS.containsKey(ext.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Tipo de archivo no permitido. Extensiones válidas: "
                    + String.join(", ", TIPOS_PERMITIDOS.keySet()));
        }
    }

    private String obtenerExtension(String nombre) {
        if (nombre == null || !nombre.contains(".")) {
            throw new IllegalArgumentException("El archivo debe tener una extensión válida");
        }
        return nombre.substring(nombre.lastIndexOf('.') + 1);
    }
}
