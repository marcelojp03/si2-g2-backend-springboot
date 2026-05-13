package com.uagrm.si2g2.institucion.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ConfiguracionParametroResponse {

    private String clave;
    private String nombre;
    private String modulo;
    private String descripcion;
    private boolean obligatorio;
    private String tipoValor;
    private String valor;
    private String valorPorDefecto;
    private boolean usaValorPorDefecto;
    private Double minimo;
    private Double maximo;
    private List<String> valoresPermitidos;
}
