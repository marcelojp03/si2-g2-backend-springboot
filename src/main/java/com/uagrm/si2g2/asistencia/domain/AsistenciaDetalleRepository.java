package com.uagrm.si2g2.asistencia.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AsistenciaDetalleRepository extends JpaRepository<AsistenciaDetalle, UUID> {

    List<AsistenciaDetalle> findAllByIdAsistenciaRegistro(UUID idAsistenciaRegistro);

    List<AsistenciaDetalle> findAllByIdAsistenciaRegistroIn(Collection<UUID> idsAsistenciaRegistro);

    boolean existsByIdAsistenciaRegistroAndIdInscripcion(UUID idAsistenciaRegistro, UUID idInscripcion);
}