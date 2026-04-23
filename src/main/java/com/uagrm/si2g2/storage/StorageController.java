package com.uagrm.si2g2.storage;

import com.uagrm.si2g2.common.dto.ApiResponse;
import com.uagrm.si2g2.storage.dto.ArchivoSubidoResponse;
import com.uagrm.si2g2.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/archivos")
@RequiredArgsConstructor
public class StorageController {

    private final S3StorageService storageService;

    /**
     * Sube un archivo a S3.
     *
     * @param archivo   Archivo (imagen o PDF), máx 10 MB
     * @param carpeta   Destino: logos | estudiantes | docentes | examenes | archivos
     */
    @PostMapping(value = "/subir", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_INSTITUCION','DIRECTOR','SECRETARIO','DOCENTE')")
    public ResponseEntity<ApiResponse<ArchivoSubidoResponse>> subir(
            @RequestPart("archivo") MultipartFile archivo,
            @RequestParam(value = "carpeta", defaultValue = "archivos") String carpeta) {

        String idInstitucion = TenantContext.get() != null
                ? TenantContext.get().toString()
                : "global";

        ArchivoSubidoResponse respuesta = storageService.subir(archivo, idInstitucion, carpeta);
        return ResponseEntity.ok(ApiResponse.ok("Archivo subido exitosamente", respuesta));
    }
}
