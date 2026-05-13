package com.uagrm.si2g2.auditoria.dto;

import com.uagrm.si2g2.auditoria.domain.BitacoraAuditoria;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class BitacoraAuditoriaResponse {

    private UUID id;
    private UUID idInstitucion;
    private UUID idUsuario;
    private Instant fechaEvento;
    private String direccionIp;
    private String plataformaCliente;
    private String agenteUsuario;
    private String metodoHttp;
    private String rutaRecurso;
    private String nombreModulo;
    private String nombreFuncion;
    private String nombreEntidad;
    private String idEntidad;
    private String tipoOperacion;
    private String datosAntes;
    private String datosDespues;
    private boolean exito;
    private String mensaje;
    private String hashIntegridad;

    public static BitacoraAuditoriaResponse from(BitacoraAuditoria bitacora) {
        return BitacoraAuditoriaResponse.builder()
                .id(bitacora.getId())
                .idInstitucion(bitacora.getIdInstitucion())
                .idUsuario(bitacora.getIdUsuario())
                .fechaEvento(bitacora.getFechaEvento())
                .direccionIp(bitacora.getDireccionIp())
                .plataformaCliente(bitacora.getPlataformaCliente())
                .agenteUsuario(bitacora.getAgenteUsuario())
                .metodoHttp(bitacora.getMetodoHttp())
                .rutaRecurso(bitacora.getRutaRecurso())
                .nombreModulo(bitacora.getNombreModulo())
                .nombreFuncion(bitacora.getNombreFuncion())
                .nombreEntidad(bitacora.getNombreEntidad())
                .idEntidad(bitacora.getIdEntidad())
                .tipoOperacion(bitacora.getTipoOperacion())
                .datosAntes(bitacora.getDatosAntes())
                .datosDespues(bitacora.getDatosDespues())
                .exito(bitacora.isExito())
                .mensaje(bitacora.getMensaje())
                .hashIntegridad(bitacora.getHashIntegridad())
                .build();
    }
}
