package com.uagrm.si2g2.respaldo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "registro_restauracion", schema = "sia")
public class RegistroRestauracion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "id_respaldo", nullable = false)
    private UUID idRespaldo;

    @Column(name = "id_institucion")
    private UUID idInstitucion;

    @Column(name = "solicitado_por")
    private UUID solicitadoPor;

    @Column(name = "aprobado_por")
    private UUID aprobadoPor;

    @Column(name = "fecha_solicitud")
    private OffsetDateTime fechaSolicitud = OffsetDateTime.now();

    @Column(name = "fecha_ejecucion")
    private OffsetDateTime fechaEjecucion;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "PENDIENTE";

    @Column(name = "motivo", columnDefinition = "TEXT", nullable = false)
    private String motivo;

    @Column(name = "observacion", columnDefinition = "TEXT")
    private String observacion;

    @Column(name = "simulado", nullable = false)
    private boolean simulado = false;

    @Column(name = "creado_en", updatable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    @Column(name = "actualizado_en")
    private OffsetDateTime actualizadoEn = OffsetDateTime.now();

    @PreUpdate
    public void onUpdate() {
        this.actualizadoEn = OffsetDateTime.now();
    }
}
