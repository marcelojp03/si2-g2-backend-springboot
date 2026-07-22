package com.uagrm.si2g2.estudiante.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;

public interface EstudianteRepository extends JpaRepository<Estudiante, UUID> {

    List<Estudiante> findAllByIdInstitucion(UUID idInstitucion);

    Optional<Estudiante> findByIdAndIdInstitucion(UUID id, UUID idInstitucion);

    List<Estudiante> findAllByIdInstitucionAndIdIn(UUID idInstitucion, Collection<UUID> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Estudiante e where e.id = :id and e.idInstitucion = :idInstitucion")
    Optional<Estudiante> findByIdAndIdInstitucionForUpdate(
            @Param("id") UUID id, @Param("idInstitucion") UUID idInstitucion);

    boolean existsByIdInstitucionAndCodigoEstudiante(UUID idInstitucion, String codigoEstudiante);

    boolean existsByIdInstitucionAndDocumentoIdentidad(UUID idInstitucion, String documentoIdentidad);

    Optional<Estudiante> findByIdUsuarioAndIdInstitucion(UUID idUsuario, UUID idInstitucion);

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
