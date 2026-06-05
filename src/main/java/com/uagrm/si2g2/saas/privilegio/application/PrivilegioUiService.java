package com.uagrm.si2g2.saas.privilegio.application;

import com.uagrm.si2g2.auth.domain.Rol;
import com.uagrm.si2g2.auth.domain.RolRepository;
import com.uagrm.si2g2.auth.domain.Usuario;
import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.saas.privilegio.domain.PrivilegioUi;
import com.uagrm.si2g2.saas.privilegio.domain.PrivilegioUiRepository;
import com.uagrm.si2g2.saas.privilegio.dto.PrivilegioUiRequest;
import com.uagrm.si2g2.saas.privilegio.dto.PrivilegioUiResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrivilegioUiService {

    private static final Set<String> VISIBILIDADES_VALIDAS = Set.of("VISIBLE", "OCULTO");
    private static final Set<String> EDICIONES_VALIDAS = Set.of("EDITABLE", "SOLO_LECTURA", "OCULTO");

    private final PrivilegioUiRepository privilegioRepository;
    private final RolRepository rolRepository;

    @Transactional(readOnly = true)
    public List<PrivilegioUiResponse> listarPorRol(UUID idRol) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        validarRolInstitucion(idRol, idInstitucion);
        return privilegioRepository.findAllByIdInstitucionAndIdRol(idInstitucion, idRol)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Retorna el mapa de privilegios del usuario autenticado en la forma:
     * { "modulo.entidad.campo": { "visibilidad": "VISIBLE", "edicion": "EDITABLE" } }
     * El frontend lo usa para decidir qué mostrar/deshabilitar sin llamar al servidor por cada campo.
     */
    @Transactional(readOnly = true)
    public Map<String, Map<String, String>> obtenerMapaUsuarioActual() {
        // Obtiene los roles del usuario actual del contexto de seguridad
        Usuario usuario = SecurityUtils.currentUser();
        if (usuario == null) {
            return Collections.emptyMap();
        }

        boolean esSuperAdmin = usuario.getRoles().stream()
                .anyMatch(rol -> "SUPER_ADMIN".equalsIgnoreCase(rol.getCodigo()));
        if (esSuperAdmin && usuario.getIdInstitucion() == null) {
            return Collections.emptyMap();
        }

        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        List<UUID> idsRoles = usuario.getRoles().stream().map(Rol::getId).toList();

        List<PrivilegioUi> privilegios = idsRoles.stream()
                .flatMap(idRol -> privilegioRepository.findAllByIdInstitucionAndIdRol(idInstitucion, idRol).stream())
                .toList();

        // Cuando hay conflicto entre roles: VISIBLE gana sobre OCULTO; EDITABLE gana sobre SOLO_LECTURA/OCULTO
        Map<String, PrivilegioUi> porClave = new LinkedHashMap<>();
        for (PrivilegioUi p : privilegios) {
            String clave = p.getModulo() + "." + p.getEntidad() + "." + p.getCampo();
            porClave.merge(clave, p, (existing, nuevo) -> {
                String mejorVis = "VISIBLE".equals(existing.getVisibilidad()) ? existing.getVisibilidad() : nuevo.getVisibilidad();
                String mejorEdit = "EDITABLE".equals(existing.getEdicion()) ? existing.getEdicion()
                        : ("SOLO_LECTURA".equals(nuevo.getEdicion()) ? nuevo.getEdicion() : existing.getEdicion());
                existing.setVisibilidad(mejorVis);
                existing.setEdicion(mejorEdit);
                return existing;
            });
        }

        return porClave.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> Map.of("visibilidad", e.getValue().getVisibilidad(), "edicion", e.getValue().getEdicion()),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    @Transactional
    public List<PrivilegioUiResponse> guardarPrivilegiosRol(UUID idRol, List<PrivilegioUiRequest> requests) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        validarRolInstitucion(idRol, idInstitucion);

        privilegioRepository.deleteAllByIdInstitucionAndIdRol(idInstitucion, idRol);

        List<PrivilegioUi> nuevos = requests.stream()
                .map(req -> {
                    validarVisibilidad(req.getVisibilidad());
                    validarEdicion(req.getEdicion());
                    return PrivilegioUi.builder()
                            .idInstitucion(idInstitucion)
                            .idRol(idRol)
                            .modulo(req.getModulo().trim().toLowerCase())
                            .entidad(req.getEntidad().trim().toLowerCase())
                            .campo(req.getCampo().trim().toLowerCase())
                            .visibilidad(req.getVisibilidad().toUpperCase())
                            .edicion(req.getEdicion().toUpperCase())
                            .build();
                })
                .toList();

        return privilegioRepository.saveAll(nuevos).stream()
                .map(this::toResponse)
                .toList();
    }

    // ── privados ────────────────────────────────────────────────────────────────

    private void validarRolInstitucion(UUID idRol, UUID idInstitucion) {
        rolRepository.findByIdAndIdInstitucion(idRol, idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Rol no encontrado en la institución: " + idRol));
    }

    private void validarVisibilidad(String v) {
        if (!VISIBILIDADES_VALIDAS.contains(v.toUpperCase())) {
            throw new IllegalArgumentException("Visibilidad inválida: " + v + ". Valores válidos: " + VISIBILIDADES_VALIDAS);
        }
    }

    private void validarEdicion(String e) {
        if (!EDICIONES_VALIDAS.contains(e.toUpperCase())) {
            throw new IllegalArgumentException("Edición inválida: " + e + ". Valores válidos: " + EDICIONES_VALIDAS);
        }
    }

    private PrivilegioUiResponse toResponse(PrivilegioUi p) {
        return new PrivilegioUiResponse(
                p.getId(), p.getIdInstitucion(), p.getIdRol(),
                p.getModulo(), p.getEntidad(), p.getCampo(),
                p.getVisibilidad(), p.getEdicion()
        );
    }
}
