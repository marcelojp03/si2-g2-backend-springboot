package com.uagrm.si2g2.aula.dto;

import com.uagrm.si2g2.aula.domain.Aula;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class AulaResponse {

    private UUID id;
    private UUID idInstitucion;
    private String codigo;
    private String nombre;
    private Integer capacidad;
    private String ubicacion;
    private List<String> recursos;
    private String estado;
    private Instant creadoEn;
    private Instant actualizadoEn;

    public static AulaResponse from(Aula aula) {
        return AulaResponse.builder()
                .id(aula.getId())
                .idInstitucion(aula.getIdInstitucion())
                .codigo(aula.getCodigo())
                .nombre(aula.getNombre())
                .capacidad(aula.getCapacidad())
                .ubicacion(aula.getUbicacion())
                .recursos(parseRecursos(aula.getRecursos()))
                .estado(aula.getEstado())
                .creadoEn(aula.getCreadoEn())
                .actualizadoEn(aula.getActualizadoEn())
                .build();
    }

    private static List<String> parseRecursos(String recursos) {
        if (recursos == null || recursos.isBlank()) {
            return List.of();
        }
        return Arrays.stream(recursos.split("\\|"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }
}
