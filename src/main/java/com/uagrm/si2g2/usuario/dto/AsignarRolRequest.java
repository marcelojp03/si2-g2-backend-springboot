package com.uagrm.si2g2.usuario.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class AsignarRolRequest {

    private UUID idRol;
    private String codigoRol;
}
