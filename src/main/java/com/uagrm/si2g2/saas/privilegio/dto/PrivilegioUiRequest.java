package com.uagrm.si2g2.saas.privilegio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class PrivilegioUiRequest {

    @NotNull
    private UUID idRol;

    @NotBlank
    private String modulo;

    @NotBlank
    private String entidad;

    @NotBlank
    private String campo;

    /** VISIBLE | OCULTO */
    @NotBlank
    private String visibilidad;

    /** EDITABLE | SOLO_LECTURA | OCULTO */
    @NotBlank
    private String edicion;
}
