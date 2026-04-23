package com.uagrm.si2g2.curso.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CursoMateriaRequest {

    @NotNull
    private UUID idMateria;
}
