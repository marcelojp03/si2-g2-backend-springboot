package com.uagrm.si2g2.docente.dto;

import com.uagrm.si2g2.docente.domain.Docente;
import com.uagrm.si2g2.materia.domain.Materia;
import com.uagrm.si2g2.materia.dto.MateriaResponse;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Builder
public class DocenteResponse {

    private UUID id;
    private UUID idInstitucion;
    private UUID idUsuario;
    private String codigo;
    private String documentoIdentidad;
    private String nombres;
    private String apellidos;
    private String telefono;
    private String correo;
    private String especialidad;
    private List<MateriaResponse> materias;
    private String estado;
    private Instant creadoEn;
    private Instant actualizadoEn;

    public static DocenteResponse from(Docente d) {
        List<MateriaResponse> materias = d.getMaterias() == null ? List.of()
                : d.getMaterias().stream()
                .sorted(Comparator.comparing(Materia::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(MateriaResponse::from)
                .collect(Collectors.toList());

        String especialidad = materias.isEmpty()
                ? d.getEspecialidad()
                : materias.stream().map(MateriaResponse::getNombre).collect(Collectors.joining(", "));

        return DocenteResponse.builder()
                .id(d.getId())
                .idInstitucion(d.getIdInstitucion())
                .idUsuario(d.getIdUsuario())
                .codigo(d.getCodigo())
                .documentoIdentidad(d.getDocumentoIdentidad())
                .nombres(d.getNombres())
                .apellidos(d.getApellidos())
                .telefono(d.getTelefono())
                .correo(d.getCorreo())
                .especialidad(especialidad)
                .materias(materias)
                .estado(d.getEstado())
                .creadoEn(d.getCreadoEn())
                .actualizadoEn(d.getActualizadoEn())
                .build();
    }
}
