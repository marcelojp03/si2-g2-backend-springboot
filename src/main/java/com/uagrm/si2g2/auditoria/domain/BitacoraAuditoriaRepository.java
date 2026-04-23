package com.uagrm.si2g2.auditoria.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BitacoraAuditoriaRepository extends JpaRepository<BitacoraAuditoria, UUID> {
}
