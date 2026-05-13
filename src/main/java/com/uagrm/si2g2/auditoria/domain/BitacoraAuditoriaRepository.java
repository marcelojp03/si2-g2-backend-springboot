package com.uagrm.si2g2.auditoria.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface BitacoraAuditoriaRepository extends JpaRepository<BitacoraAuditoria, UUID>, JpaSpecificationExecutor<BitacoraAuditoria> {

    List<BitacoraAuditoria> findTop200ByOrderByFechaEventoDesc();

    List<BitacoraAuditoria> findTop200ByIdInstitucionOrderByFechaEventoDesc(UUID idInstitucion);
}
