package com.uagrm.si2g2.auth.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    List<Usuario> findAllByIdInstitucion(UUID idInstitucion);

    Optional<Usuario> findByIdAndIdInstitucion(UUID id, UUID idInstitucion);

    long countByEstado(String estado);

    long countByIdInstitucion(UUID idInstitucion);

    long countByIdInstitucionAndEstado(UUID idInstitucion, String estado);

    @Query("""
            select distinct u.idInstitucion
            from Usuario u join u.roles r
            where u.idInstitucion is not null
              and u.estado = :estado
              and r.codigo = :codigoRol
            """)
    List<UUID> findInstitutionIdsWithActiveRole(@Param("codigoRol") String codigoRol,
                                                @Param("estado") String estado);

    @Query("""
            select u from Usuario u join u.roles r
            where u.idInstitucion = :idInstitucion
              and u.estado = 'ACTIVO'
              and r.codigo in :codigosRoles
            """)
    List<Usuario> findByIdInstitucionAndRoles(@Param("idInstitucion") UUID idInstitucion,
                                              @Param("codigosRoles") List<String> codigosRoles);
}
