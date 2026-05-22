package com.uagrm.si2g2.calificacion.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CalificacionRepository extends JpaRepository<Calificacion, UUID> {

    List<Calificacion> findAllByIdEvaluacion(UUID idEvaluacion);

    List<Calificacion> findAllByIdEvaluacionIn(Collection<UUID> idsEvaluacion);

    Optional<Calificacion> findByIdEvaluacionAndIdInscripcion(UUID idEvaluacion, UUID idInscripcion);
}
