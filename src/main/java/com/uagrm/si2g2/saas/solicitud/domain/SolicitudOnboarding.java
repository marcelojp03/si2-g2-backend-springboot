package com.uagrm.si2g2.saas.solicitud.domain;

import com.uagrm.si2g2.saas.plan.domain.PlanSuscripcion;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "solicitud_onboarding")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class SolicitudOnboarding {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "nombre_institucion", nullable = false, length = 200)
    private String nombreInstitucion;

    @Builder.Default
    @Column(name = "tipo_institucion", nullable = false, length = 20)
    private String tipoInstitucion = "PRIVADO";

    @Column(name = "telefono_institucion", length = 30)
    private String telefonoInstitucion;

    @Column(name = "correo_institucion")
    private String correoInstitucion;

    @Column(name = "direccion_institucion", length = 255)
    private String direccionInstitucion;

    @Column(name = "nombres_contacto", nullable = false, length = 120)
    private String nombresContacto;

    @Column(name = "apellidos_contacto", nullable = false, length = 120)
    private String apellidosContacto;

    @Column(name = "correo_contacto", nullable = false)
    private String correoContacto;

    @Column(name = "telefono_contacto", length = 30)
    private String telefonoContacto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_plan", nullable = false)
    private PlanSuscripcion plan;

    @Column(name = "mensaje", columnDefinition = "TEXT")
    private String mensaje;

    @Builder.Default
    @Column(name = "estado", nullable = false, length = 30)
    private String estado = "PENDIENTE_REVISION";

    @Column(name = "notas_admin", columnDefinition = "TEXT")
    private String notasAdmin;

    @Column(name = "id_institucion_creada")
    private UUID idInstitucionCreada;

    @Column(name = "id_usuario_creado")
    private UUID idUsuarioCreado;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @PrePersist
    protected void onCreate() {
        creadoEn = Instant.now();
        actualizadoEn = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        actualizadoEn = Instant.now();
    }
}
