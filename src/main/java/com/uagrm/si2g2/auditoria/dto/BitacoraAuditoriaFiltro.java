package com.uagrm.si2g2.auditoria.dto;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class BitacoraAuditoriaFiltro {

    private String modulo;
    private String tipoOperacion;
    private Boolean exito;
    private UUID idUsuario;
    private Instant fechaDesde;
    private Instant fechaHasta;
}
