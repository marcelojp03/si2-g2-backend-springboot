package com.uagrm.si2g2.storage.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class ArchivoUploadRequest {
    private String modulo;
    private String entidad;
    private UUID idEntidad;
    private String tipoReferencia;
    private Boolean esPrincipal = true;
    private String observacion;
}
