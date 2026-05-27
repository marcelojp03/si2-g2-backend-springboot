package com.uagrm.si2g2.asignacion.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AsignacionDocenteRepository extends JpaRepository<AsignacionDocente, UUID> {

        List<AsignacionDocente> findAllByIdInstitucion(UUID idInstitucion);

        List<AsignacionDocente> findAllByIdInstitucionAndIdParalelo(UUID idInstitucion, UUID idParalelo);

        List<AsignacionDocente> findAllByIdInstitucionAndIdGestion(UUID idInstitucion, UUID idGestion);

        List<AsignacionDocente> findAllByIdInstitucionAndIdDocente(UUID idInstitucion, UUID idDocente);

        Optional<AsignacionDocente> findByIdAndIdInstitucion(UUID id, UUID idInstitucion);

        boolean existsByIdInstitucionAndIdDocenteAndIdMateriaAndIdParaleloAndIdGestion(
                        UUID idInstitucion, UUID idDocente, UUID idMateria, UUID idParalelo, UUID idGestion);

        boolean existsByIdDocenteAndIdMateriaAndEstado(UUID idDocente, UUID idMateria, String estado);

        List<AsignacionDocente> findByIdMateriaAndIdInstitucionAndEstado(UUID idMateria, UUID idInstitucion,
                        String estado);

        long countByIdInstitucion(UUID idInstitucion);

        long countByIdInstitucionAndEstado(UUID idInstitucion, String estado);

        long countByIdInstitucionAndIdGestionAndEstado(UUID idInstitucion, UUID idGestion, String estado);

        @Query("""
                        select count(distinct a.idDocente)
                        from AsignacionDocente a
                        where a.idInstitucion = :idInstitucion
                          and a.estado = :estado
                        """)
        long countDistinctDocentesByInstitutionAndEstado(@Param("idInstitucion") UUID idInstitucion,
                        @Param("estado") String estado);
}
