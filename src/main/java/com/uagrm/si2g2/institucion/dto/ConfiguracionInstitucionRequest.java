package com.uagrm.si2g2.institucion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConfiguracionInstitucionRequest {

    @NotBlank
    @Size(max = 100)
    private String clave;

    @NotBlank
    private String valor;

    @Pattern(regexp = "TEXTO|NUMERO|BOOLEANO|JSON", message = "Debe ser TEXTO, NUMERO, BOOLEANO o JSON")
    private String tipoValor = "TEXTO";

    @Size(max = 255)
    private String descripcion;
}
