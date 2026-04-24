package com.uagrm.si2g2.institucion.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InstitucionRequest {

    @NotBlank
    @Size(max = 30)
    private String codigo;

    @NotBlank
    @Size(max = 200)
    private String nombre;

    @NotBlank
    @Pattern(regexp = "FISCAL|CONVENIO|PRIVADO", message = "Debe ser FISCAL, CONVENIO o PRIVADO")
    private String tipoInstitucion;

    @Size(max = 30)
    private String telefono;

    @Email
    private String correo;

    @Size(max = 255)
    private String direccion;
}
