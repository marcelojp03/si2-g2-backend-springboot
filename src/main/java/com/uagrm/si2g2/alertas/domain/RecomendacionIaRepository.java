package com.uagrm.si2g2.alertas.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RecomendacionIaRepository extends JpaRepository<RecomendacionIa, UUID> {

    @Query("""
            select r from RecomendacionIa r, AlertaRiesgo a
            where r.idAlertaRiesgo = a.id
              and a.id = :idAlerta
              and a.idInstitucion = :idInstitucion
            order by r.creadoEn desc
            """)
    List<RecomendacionIa> findByAlertaAndInstitucion(
            @Param("idAlerta") UUID idAlerta, @Param("idInstitucion") UUID idInstitucion);

    void deleteAllByIdAlertaRiesgo(UUID idAlertaRiesgo);
}
