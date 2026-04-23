package com.uagrm.si2g2.estudiante.dto;

import com.uagrm.si2g2.estudiante.domain.Estudiante;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class EstudianteResponse {

    private UUID id;
    private UUID idInstitucion;
    private UUID idUsuario;
    private String codigoEstudiante;
    private String documentoIdentidad;
    private String nombres;
    private String apellidos;
    private LocalDate fechaNacimiento;
    private String sexo;
    private String direccion;
    private String telefono;
    private String correo;
    private String estado;
    private Instant creadoEn;
    private Instant actualizadoEn;

    public static EstudianteResponse from(Estudiante e) {
        return EstudianteResponse.builder()
                .id(e.getId())
                .idInstitucion(e.getIdInstitucion())
                .idUsuario(e.getIdUsuario())
                .codigoEstudiante(e.getCodigoEstudiante())
                .documentoIdentidad(e.getDocumentoIdentidad())
                .nombres(e.getNombres())
                .apellidos(e.getApellidos())
                .fechaNacimiento(e.getFechaNacimiento())
                .sexo(e.getSexo())
                .direccion(e.getDireccion())
                .telefono(e.getTelefono())
                .correo(e.getCorreo())
                .estado(e.getEstado())
                .creadoEn(e.getCreadoEn())
                .actualizadoEn(e.getActualizadoEn())
                .build();
    }
}
