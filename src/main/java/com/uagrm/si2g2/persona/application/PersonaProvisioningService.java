package com.uagrm.si2g2.persona.application;

import com.uagrm.si2g2.auth.domain.Rol;
import com.uagrm.si2g2.auth.domain.Usuario;
import com.uagrm.si2g2.auth.domain.UsuarioRepository;
import com.uagrm.si2g2.docente.domain.Docente;
import com.uagrm.si2g2.docente.domain.DocenteRepository;
import com.uagrm.si2g2.estudiante.domain.Estudiante;
import com.uagrm.si2g2.estudiante.domain.EstudianteRepository;
import com.uagrm.si2g2.tutor.domain.Tutor;
import com.uagrm.si2g2.tutor.domain.TutorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

/**
 * Crea el perfil académico (docente, estudiante, tutor) cuando un usuario recibe
 * ese rol desde Usuarios, sin pasar por el módulo de Personas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PersonaProvisioningService {

    private static final Set<String> PERSONA_ROLES = Set.of("DOCENTE", "ESTUDIANTE", "TUTOR");

    private final DocenteRepository docenteRepository;
    private final EstudianteRepository estudianteRepository;
    private final TutorRepository tutorRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public void provisionForUsuario(Usuario usuario) {
        if (usuario == null || usuario.getIdInstitucion() == null || usuario.getRoles() == null) {
            return;
        }
        for (Rol rol : usuario.getRoles()) {
            switch (rol.getCodigo()) {
                case "DOCENTE" -> provisionDocente(usuario);
                case "ESTUDIANTE" -> provisionEstudiante(usuario);
                case "TUTOR" -> provisionTutor(usuario);
                default -> { /* roles administrativos no requieren perfil */ }
            }
        }
    }

    @Transactional
    public void syncMissingProfiles() {
        usuarioRepository.findAll().stream()
                .filter(u -> u.getIdInstitucion() != null && u.getRoles() != null)
                .filter(u -> u.getRoles().stream().anyMatch(r -> PERSONA_ROLES.contains(r.getCodigo())))
                .forEach(this::provisionForUsuario);
    }

    private void provisionDocente(Usuario usuario) {
        UUID idInstitucion = usuario.getIdInstitucion();
        if (docenteRepository.findByIdUsuarioAndIdInstitucion(usuario.getId(), idInstitucion).isPresent()) {
            return;
        }
        String codigo = uniqueCodigo("DOC", usuario.getId(),
                candidate -> docenteRepository.existsByIdInstitucionAndCodigo(idInstitucion, candidate));
        String documento = uniqueDocumento(usuario.getId(), idInstitucion,
                candidate -> docenteRepository.existsByIdInstitucionAndDocumentoIdentidad(idInstitucion, candidate));

        Docente docente = Docente.builder()
                .idInstitucion(idInstitucion)
                .idUsuario(usuario.getId())
                .codigo(codigo)
                .documentoIdentidad(documento)
                .nombres(usuario.getNombres())
                .apellidos(usuario.getApellidos())
                .telefono(usuario.getTelefono())
                .correo(usuario.getCorreo())
                .build();
        docenteRepository.save(docente);
        log.info("Perfil docente provisionado para usuario {} ({})", usuario.getCorreo(), codigo);
    }

    private void provisionEstudiante(Usuario usuario) {
        UUID idInstitucion = usuario.getIdInstitucion();
        if (estudianteRepository.findByIdUsuarioAndIdInstitucion(usuario.getId(), idInstitucion).isPresent()) {
            return;
        }
        String codigo = uniqueCodigo("EST", usuario.getId(),
                candidate -> estudianteRepository.existsByIdInstitucionAndCodigoEstudiante(idInstitucion, candidate));
        String documento = uniqueDocumento(usuario.getId(), idInstitucion,
                candidate -> estudianteRepository.existsByIdInstitucionAndDocumentoIdentidad(idInstitucion, candidate));

        Estudiante estudiante = Estudiante.builder()
                .idInstitucion(idInstitucion)
                .idUsuario(usuario.getId())
                .codigoEstudiante(codigo)
                .documentoIdentidad(documento)
                .nombres(usuario.getNombres())
                .apellidos(usuario.getApellidos())
                .telefono(usuario.getTelefono())
                .correo(usuario.getCorreo())
                .build();
        estudianteRepository.save(estudiante);
        log.info("Perfil estudiante provisionado para usuario {} ({})", usuario.getCorreo(), codigo);
    }

    private void provisionTutor(Usuario usuario) {
        UUID idInstitucion = usuario.getIdInstitucion();
        if (tutorRepository.findByIdUsuarioAndIdInstitucion(usuario.getId(), idInstitucion).isPresent()) {
            return;
        }
        String documento = uniqueDocumento(usuario.getId(), idInstitucion,
                candidate -> tutorRepository.existsByIdInstitucionAndDocumentoIdentidad(idInstitucion, candidate));

        Tutor tutor = Tutor.builder()
                .idInstitucion(idInstitucion)
                .idUsuario(usuario.getId())
                .documentoIdentidad(documento)
                .nombres(usuario.getNombres())
                .apellidos(usuario.getApellidos())
                .telefono(usuario.getTelefono())
                .correo(usuario.getCorreo())
                .build();
        tutorRepository.save(tutor);
        log.info("Perfil tutor provisionado para usuario {}", usuario.getCorreo());
    }

    private static String uniqueCodigo(String prefix, UUID sourceId, java.util.function.Predicate<String> exists) {
        String base = prefix + "-" + sourceId.toString().replace("-", "").substring(0, 8).toUpperCase();
        String candidate = base;
        int suffix = 1;
        while (exists.test(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate.length() > 30 ? candidate.substring(0, 30) : candidate;
    }

    private static String uniqueDocumento(UUID userId, UUID idInstitucion, java.util.function.Predicate<String> exists) {
        String base = "USR-" + userId.toString().replace("-", "").substring(0, 12).toUpperCase();
        String candidate = base;
        int suffix = 1;
        while (exists.test(candidate)) {
            candidate = base + suffix++;
            if (candidate.length() > 30) {
                candidate = candidate.substring(0, 30);
            }
        }
        return candidate;
    }
}
