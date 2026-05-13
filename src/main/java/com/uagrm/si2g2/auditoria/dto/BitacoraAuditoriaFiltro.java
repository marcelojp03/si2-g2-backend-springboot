package com.uagrm.si2g2.auditoria.dto;

import lombok.Data;

@Data
public class BitacoraAuditoriaFiltro {

    private String modulo;
    private String tipoOperacion;
    private Boolean exito;
}
