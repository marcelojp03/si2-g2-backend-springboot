package com.uagrm.si2g2.comunicado.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ComunicadoRequest {

    @NotBlank
    @Size(max = 200)
    private String titulo;

    @NotBlank
    private String contenido;

    @Size(max = 30)
    private String tipo;

    @Size(max = 50)
    private String destinatarios;
}
