package com.uagrm.si2g2.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AsignarRolRequest {

    @NotBlank
    private String codigoRol;
}
