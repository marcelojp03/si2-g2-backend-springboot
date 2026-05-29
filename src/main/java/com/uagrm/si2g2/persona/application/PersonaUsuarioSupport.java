package com.uagrm.si2g2.persona.application;

import com.uagrm.si2g2.auth.domain.Rol;
import com.uagrm.si2g2.auth.domain.RolRepository;
import com.uagrm.si2g2.auth.domain.Usuario;
import com.uagrm.si2g2.auth.domain.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PersonaUsuarioSupport {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Reutiliza un usuario existente con el rol esperado o crea uno nuevo para el módulo de personas.
     */
    public Usuario resolveOrCreate(
            UUID idInstitucion,
            String correo,
            String codigoRol,
            String nombres,
            String apellidos,
            String telefono,
            String contrasenaInicial
    ) {
        return usuarioRepository.findByCorreo(correo)
                .map(usuario -> validateExisting(usuario, idInstitucion, codigoRol))
                .orElseGet(() -> createNew(idInstitucion, correo, codigoRol, nombres, apellidos, telefono, contrasenaInicial));
    }

    private Usuario validateExisting(Usuario usuario, UUID idInstitucion, String codigoRol) {
        if (usuario.getIdInstitucion() == null || !idInstitucion.equals(usuario.getIdInstitucion())) {
            throw new IllegalStateException("El correo ya está registrado en otra institución");
        }
        boolean hasRole = usuario.getRoles().stream().anyMatch(rol -> codigoRol.equals(rol.getCodigo()));
        if (!hasRole) {
            throw new IllegalStateException("El usuario existe pero no tiene el rol " + codigoRol);
        }
        return usuario;
    }

    private Usuario createNew(
            UUID idInstitucion,
            String correo,
            String codigoRol,
            String nombres,
            String apellidos,
            String telefono,
            String contrasenaInicial
    ) {
        Rol rol = rolRepository.findByCodigo(codigoRol)
                .orElseThrow(() -> new IllegalStateException("Rol no encontrado: " + codigoRol));
        Usuario usuario = Usuario.builder()
                .idInstitucion(idInstitucion)
                .correo(correo)
                .hashContrasena(passwordEncoder.encode(contrasenaInicial))
                .nombres(nombres)
                .apellidos(apellidos)
                .telefono(telefono)
                .roles(Set.of(rol))
                .requiereCambioContrasena(false)
                .build();
        return usuarioRepository.save(usuario);
    }
}
