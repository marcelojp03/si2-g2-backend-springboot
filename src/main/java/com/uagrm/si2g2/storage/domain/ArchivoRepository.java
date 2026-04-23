package com.uagrm.si2g2.storage.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ArchivoRepository extends JpaRepository<Archivo, UUID> {
}
