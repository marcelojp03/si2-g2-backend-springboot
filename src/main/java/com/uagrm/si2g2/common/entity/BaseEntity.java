package com.uagrm.si2g2.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {

    @Column(name = "estado", nullable = false)
    protected String estado = "ACTIVO";

    @Column(name = "creado_en", nullable = false, updatable = false)
    protected Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    protected Instant actualizadoEn;

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
