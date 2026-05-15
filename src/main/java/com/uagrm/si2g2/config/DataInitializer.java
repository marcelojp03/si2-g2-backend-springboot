package com.uagrm.si2g2.config;

import com.uagrm.si2g2.auth.domain.Rol;
import com.uagrm.si2g2.auth.domain.RolRepository;
import com.uagrm.si2g2.auth.domain.Usuario;
import com.uagrm.si2g2.auth.domain.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AppProperties.SuperAdmin superAdminProperties = appProperties.getSuperAdmin();

        if (usuarioRepository.findByCorreo(superAdminProperties.getCorreo()).isPresent()) {
            log.info("SUPER_ADMIN ya existe, omitiendo inicialización.");
            return;
        }

        Rol rol = rolRepository.findByCodigo("SUPER_ADMIN")
                .orElseThrow(() -> new IllegalStateException(
                        "Rol SUPER_ADMIN no encontrado. Ejecute db-script.sql primero."));

        Usuario superAdmin = Usuario.builder()
                .idInstitucion(null)
                .correo(superAdminProperties.getCorreo())
                .hashContrasena(passwordEncoder.encode(superAdminProperties.getContrasena()))
                .nombres(superAdminProperties.getNombres())
                .apellidos(superAdminProperties.getApellidos())
                .roles(Set.of(rol))
                .build();

        usuarioRepository.save(superAdmin);
        log.info("SUPER_ADMIN creado con correo: {}", superAdminProperties.getCorreo());
    }
}
