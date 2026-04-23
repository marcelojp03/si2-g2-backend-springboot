package com.uagrm.si2g2.estudiante.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class EstudianteRequest {

    @NotBlank
    @Size(max = 30)
    private String codigoEstudiante;

    @Size(max = 20)
    private String documentoIdentidad;

    @NotBlank
    @Size(max = 100)
    private String nombres;

    @NotBlank
    @Size(max = 100)
    private String apellidos;

    private LocalDate fechaNacimiento;

    @Pattern(regexp = "^[MFO]$", message = "Sexo debe ser M, F u O")
    private String sexo;

    private String direccion;

    @Size(max = 20)
    private String telefono;

    @Email
    @Size(max = 150)
    private String correo;

    /** Opcional: vincular con usuario del sistema */
    private UUID idUsuario;
}
