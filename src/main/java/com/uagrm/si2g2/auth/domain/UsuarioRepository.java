package com.uagrm.si2g2.auth.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    List<Usuario> findAllByIdInstitucion(UUID idInstitucion);

    Optional<Usuario> findByIdAndIdInstitucion(UUID id, UUID idInstitucion);
}
