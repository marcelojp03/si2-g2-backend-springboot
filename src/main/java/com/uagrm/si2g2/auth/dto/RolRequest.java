package com.uagrm.si2g2.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class RolRequest {

    @NotBlank
    private String nombre;

    private String descripcion;

    @NotEmpty
    private List<UUID> idsPermiso;
}
