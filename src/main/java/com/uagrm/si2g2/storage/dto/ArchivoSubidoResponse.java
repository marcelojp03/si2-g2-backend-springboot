package com.uagrm.si2g2.storage.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ArchivoSubidoResponse {
    private String url;
    private String nombre;
    private String tipo;
    private long tamanioBytes;
}
