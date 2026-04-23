package com.uagrm.si2g2.institucion.dto;

import com.uagrm.si2g2.institucion.domain.ConfiguracionInstitucion;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ConfiguracionInstitucionResponse {

    private UUID id;
    private UUID idInstitucion;
    private String clave;
    private String valor;
    private String tipoValor;
    private String descripcion;

    public static ConfiguracionInstitucionResponse from(ConfiguracionInstitucion c) {
        return ConfiguracionInstitucionResponse.builder()
                .id(c.getId())
                .idInstitucion(c.getIdInstitucion())
                .clave(c.getClave())
                .valor(c.getValor())
                .tipoValor(c.getTipoValor())
                .descripcion(c.getDescripcion())
                .build();
    }
}
