package com.uagrm.si2g2.calificacion.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CalificacionCambioRepository extends JpaRepository<CalificacionCambio, UUID> {

    List<CalificacionCambio> findAllByIdCalificacionOrderByFechaCambioDesc(UUID idCalificacion);
}
