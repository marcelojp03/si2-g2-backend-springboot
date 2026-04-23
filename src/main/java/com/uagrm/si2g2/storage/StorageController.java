package com.uagrm.si2g2.storage;

import com.uagrm.si2g2.auth.domain.Usuario;
import com.uagrm.si2g2.common.dto.ApiResponse;
import com.uagrm.si2g2.storage.dto.ArchivoResponse;
import com.uagrm.si2g2.storage.dto.ArchivoUploadRequest;
import com.uagrm.si2g2.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/archivos")
@RequiredArgsConstructor
public class StorageController {

    private final ArchivoService archivoService;

    /**
     * Sube un archivo a S3, lo registra en BD y crea la referencia con la entidad indicada.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_INSTITUCION','DIRECTOR','SECRETARIO','DOCENTE')")
    public ResponseEntity<ApiResponse<ArchivoResponse>> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam("modulo") String modulo,
            @RequestParam("entidad") String entidad,
            @RequestParam("idEntidad") UUID idEntidad,
            @RequestParam("tipoReferencia") String tipoReferencia,
            @RequestParam(value = "esPrincipal", defaultValue = "true") Boolean esPrincipal,
            @RequestParam(value = "observacion", required = false) String observacion,
            @AuthenticationPrincipal Usuario usuario) {

        UUID idInstitucion = TenantContext.get();
        UUID idUsuario = usuario != null ? usuario.getId() : null;

        ArchivoUploadRequest req = new ArchivoUploadRequest();
        req.setModulo(modulo.toUpperCase());
        req.setEntidad(entidad.toLowerCase());
        req.setIdEntidad(idEntidad);
        req.setTipoReferencia(tipoReferencia.toUpperCase());
        req.setEsPrincipal(esPrincipal);
        req.setObservacion(observacion);

        ArchivoResponse response = archivoService.subirYRegistrar(file, idInstitucion, idUsuario, req);
        return ResponseEntity.ok(ApiResponse.ok("Archivo subido y registrado exitosamente", response));
    }

    /**
     * Lista los archivos activos asociados a una entidad.
     */
    @GetMapping("/entidad")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_INSTITUCION','DIRECTOR','SECRETARIO','DOCENTE')")
    public ResponseEntity<ApiResponse<List<ArchivoResponse>>> listarPorEntidad(
            @RequestParam("modulo") String modulo,
            @RequestParam("entidad") String entidad,
            @RequestParam("idEntidad") UUID idEntidad) {

        UUID idInstitucion = TenantContext.get();
        List<ArchivoResponse> lista = archivoService.listarPorEntidad(
                idInstitucion, modulo.toUpperCase(), entidad.toLowerCase(), idEntidad);
        return ResponseEntity.ok(ApiResponse.ok("Archivos obtenidos", lista));
    }

    /**
     * Obtiene el archivo principal activo de un tipo para una entidad.
     */
    @GetMapping("/principal")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_INSTITUCION','DIRECTOR','SECRETARIO','DOCENTE')")
    public ResponseEntity<ApiResponse<ArchivoResponse>> obtenerPrincipal(
            @RequestParam("modulo") String modulo,
            @RequestParam("entidad") String entidad,
            @RequestParam("idEntidad") UUID idEntidad,
            @RequestParam("tipoReferencia") String tipoReferencia) {

        UUID idInstitucion = TenantContext.get();
        return archivoService.obtenerPrincipal(
                        idInstitucion, modulo.toUpperCase(), entidad.toLowerCase(), idEntidad, tipoReferencia.toUpperCase())
                .map(r -> ResponseEntity.ok(ApiResponse.ok("Archivo principal encontrado", r)))
                .orElse(ResponseEntity.ok(ApiResponse.ok("Sin archivo principal", null)));
    }

    /**
     * Eliminación lógica de un archivo.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_INSTITUCION','DIRECTOR')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable UUID id) {
        UUID idInstitucion = TenantContext.get();
        archivoService.eliminar(id, idInstitucion);
        return ResponseEntity.ok(ApiResponse.ok("Archivo eliminado", null));
    }
}
