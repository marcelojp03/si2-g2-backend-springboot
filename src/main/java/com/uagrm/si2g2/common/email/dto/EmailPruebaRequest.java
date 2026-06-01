package com.uagrm.si2g2.common.email.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EmailPruebaRequest {

    @NotBlank
    @Email
    @Size(max = 150)
    private String destinatario;

    @NotBlank
    @Size(max = 160)
    private String asunto;

    @NotBlank
    @Size(max = 4000)
    private String mensaje;
}
