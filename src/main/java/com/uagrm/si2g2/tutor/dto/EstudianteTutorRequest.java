package com.uagrm.si2g2.tutor.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class EstudianteTutorRequest {

    @NotNull
    private UUID idTutor;

    @Size(max = 50)
    private String parentesco;

    private boolean esPrincipal = false;
}
