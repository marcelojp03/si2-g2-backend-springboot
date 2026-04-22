package com.uagrm.si2g2.auth.application;

import com.uagrm.si2g2.auth.domain.Rol;
import com.uagrm.si2g2.auth.domain.RolRepository;
import com.uagrm.si2g2.auth.domain.Usuario;
import com.uagrm.si2g2.auth.domain.UsuarioRepository;
import com.uagrm.si2g2.auth.dto.AuthResponse;
import com.uagrm.si2g2.auth.dto.LoginRequest;
import com.uagrm.si2g2.auth.dto.RegisterRequest;
import com.uagrm.si2g2.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        Rol rol = rolRepository.findByCodigo("ADMIN_INSTITUCION")
                .orElseThrow(() -> new IllegalStateException(
                        "Rol ADMIN_INSTITUCION no encontrado. Ejecute db-script.sql primero."));

        Usuario usuario = Usuario.builder()
                .idInstitucion(request.getIdInstitucion())
                .correo(request.getCorreo())
                .hashContrasena(passwordEncoder.encode(request.getContrasena()))
                .nombres(request.getNombres())
                .apellidos(request.getApellidos())
                .roles(Set.of(rol))
                .build();

        usuarioRepository.save(usuario);

        return buildAuthResponse(usuario);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getCorreo(), request.getContrasena())
        );

        Usuario usuario = usuarioRepository.findByCorreo(request.getCorreo())
                .orElseThrow();

        return buildAuthResponse(usuario);
    }

    private AuthResponse buildAuthResponse(Usuario usuario) {
        List<String> roles = usuario.getRoles().stream()
                .map(Rol::getCodigo)
                .collect(Collectors.toList());

        Map<String, Object> claims = new HashMap<>();
        if (usuario.getIdInstitucion() != null) {
            claims.put("id_institucion", usuario.getIdInstitucion().toString());
        }
        claims.put("roles", roles);

        String token = jwtService.generateToken(claims, usuario);

        return AuthResponse.builder()
                .token(token)
                .correo(usuario.getCorreo())
                .roles(roles)
                .requiereCambioContrasena(usuario.isRequiereCambioContrasena())
                .build();
    }
}
