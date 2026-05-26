package com.uagrm.si2g2.saas.solicitud.dto;

import jakarta.validation.constraints.*;

public record SolicitudOnboardingRequest(
        @NotBlank @Size(max = 200) String nombreInstitucion,
        @NotBlank @Pattern(regexp = "FISCAL|CONVENIO|PRIVADO") String tipoInstitucion,
        @Size(max = 30) String telefonoInstitucion,
        @Email @Size(max = 150) String correoInstitucion,
        @Size(max = 255) String direccionInstitucion,
        @NotBlank @Size(max = 120) String nombresContacto,
        @NotBlank @Size(max = 120) String apellidosContacto,
        @NotBlank @Email @Size(max = 150) String correoContacto,
        @Size(max = 30) String telefonoContacto,
        @NotNull java.util.UUID idPlan,
        @Size(max = 1000) String mensaje
) {}
