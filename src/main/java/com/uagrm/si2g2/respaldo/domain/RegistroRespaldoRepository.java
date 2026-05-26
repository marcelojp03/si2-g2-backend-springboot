package com.uagrm.si2g2.respaldo.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RegistroRespaldoRepository extends JpaRepository<RegistroRespaldo, UUID> {

    List<RegistroRespaldo> findAllByIdInstitucionOrderByFechaInicioDesc(UUID idInstitucion);

    List<RegistroRespaldo> findAllByOrderByFechaInicioDesc();
}
