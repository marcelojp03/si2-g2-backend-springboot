package com.uagrm.si2g2.academico.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GestionAcademicaRepository extends JpaRepository<GestionAcademica, UUID> {

    List<GestionAcademica> findAllByIdInstitucion(UUID idInstitucion);

    Optional<GestionAcademica> findByIdAndIdInstitucion(UUID id, UUID idInstitucion);

    boolean existsByIdInstitucionAndNombre(UUID idInstitucion, String nombre);

    Optional<GestionAcademica> findByIdInstitucionAndActivaTrue(UUID idInstitucion);

    @Modifying
    @Query("UPDATE GestionAcademica g SET g.activa = false WHERE g.idInstitucion = :idInstitucion AND g.activa = true")
    void desactivarTodasPorInstitucion(UUID idInstitucion);
}
