package com.uagrm.si2g2.estudiante.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EstudianteRepository extends JpaRepository<Estudiante, UUID> {

    List<Estudiante> findAllByIdInstitucion(UUID idInstitucion);

    Optional<Estudiante> findByIdAndIdInstitucion(UUID id, UUID idInstitucion);

    boolean existsByIdInstitucionAndCodigoEstudiante(UUID idInstitucion, String codigoEstudiante);

    boolean existsByIdInstitucionAndDocumentoIdentidad(UUID idInstitucion, String documentoIdentidad);

    long countByIdInstitucion(UUID idInstitucion);

    long countByIdInstitucionAndEstado(UUID idInstitucion, String estado);

    long countByIdInstitucionAndSexo(UUID idInstitucion, String sexo);

    @Query("""
            select count(e)
            from Estudiante e
            where e.idInstitucion = :idInstitucion
              and e.estado = 'ACTIVO'
              and not exists (
                  select et.id
                  from EstudianteTutor et
                  where et.idInstitucion = e.idInstitucion
                    and et.idEstudiante = e.id
                    and et.estado = 'ACTIVO'
              )
            """)
    long countActiveWithoutTutor(@Param("idInstitucion") UUID idInstitucion);
}
