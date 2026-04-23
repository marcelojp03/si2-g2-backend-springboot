package com.uagrm.si2g2.storage.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class ArchivoResponse {
    private UUID id;
    private String nombreOriginal;
    private String mimeType;
    private Long tamanoBytes;
    private String extension;
    private String categoria;
    private String visibilidad;
    private String url;

    // Datos de la referencia
    private String modulo;
    private String entidad;
    private UUID idEntidad;
    private String tipoReferencia;
    private Boolean esPrincipal;
    private Instant creadoEn;
}
