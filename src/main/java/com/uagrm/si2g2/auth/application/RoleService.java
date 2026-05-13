package com.uagrm.si2g2.auth.application;

import com.uagrm.si2g2.auditoria.application.AuditoriaService;
import com.uagrm.si2g2.auth.domain.Permiso;
import com.uagrm.si2g2.auth.domain.PermisoRepository;
import com.uagrm.si2g2.auth.domain.Rol;
import com.uagrm.si2g2.auth.domain.RolRepository;
import com.uagrm.si2g2.auth.dto.PermisoResponse;
import com.uagrm.si2g2.auth.dto.RolRequest;
import com.uagrm.si2g2.auth.dto.RolResponse;
import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;
    private final AuditoriaService auditoriaService;

    @Transactional(readOnly = true)
    public List<PermisoResponse> listarPermisos() {
        return permisoRepository.findAllByEstadoOrderByModuloAscAccionAsc("ACTIVO")
                .stream()
                .map(PermisoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RolResponse> listarRolesDisponibles() {
        if (SecurityUtils.currentUserHasRole("SUPER_ADMIN")) {
            return rolRepository.findAll().stream()
                    .map(rol -> RolResponse.from(rol, false))
                    .toList();
        }

        UUID idInstitucion = requireInstitutionTenant();
        return buildInstitutionRoleList(idInstitucion);
    }

    @Transactional(readOnly = true)
    public List<RolResponse> listarRolesAsignables() {
        return listarRolesDisponibles();
    }

    @Transactional
    public RolResponse crearRolInstitucional(RolRequest request) {
        UUID idInstitucion = requireInstitutionTenant();

        if (rolRepository.existsByIdInstitucionAndNombreIgnoreCase(idInstitucion, request.getNombre())) {
            throw new IllegalStateException("Ya existe un rol institucional con el nombre: " + request.getNombre());
        }

        Rol rol = Rol.builder()
                .codigo(generateRoleCode(idInstitucion, request.getNombre()))
                .nombre(request.getNombre().trim())
                .descripcion(request.getDescripcion())
                .idInstitucion(idInstitucion)
                .esGlobal(false)
                .permisos(loadPermisos(request.getIdsPermiso()))
                .build();

        Rol saved = rolRepository.save(rol);
        auditoriaService.registrar(idInstitucion, SecurityUtils.currentUserId(),
                "ROL", "CREAR", "rol", saved.getId().toString(), true,
                "Rol institucional creado: " + saved.getNombre());
        return RolResponse.from(saved, true);
    }

    @Transactional
    public RolResponse actualizarRolInstitucional(UUID idRol, RolRequest request) {
        UUID idInstitucion = requireInstitutionTenant();
        Rol rol = rolRepository.findByIdAndIdInstitucion(idRol, idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Rol institucional no encontrado: " + idRol));

        if (!rol.getNombre().equalsIgnoreCase(request.getNombre())
                && rolRepository.existsByIdInstitucionAndNombreIgnoreCase(idInstitucion, request.getNombre())) {
            throw new IllegalStateException("Ya existe un rol institucional con el nombre: " + request.getNombre());
        }

        rol.setNombre(request.getNombre().trim());
        rol.setDescripcion(request.getDescripcion());
        rol.setPermisos(loadPermisos(request.getIdsPermiso()));

        Rol saved = rolRepository.save(rol);
        auditoriaService.registrar(idInstitucion, SecurityUtils.currentUserId(),
                "ROL", "ACTUALIZAR", "rol", saved.getId().toString(), true,
                "Rol institucional actualizado: " + saved.getNombre());
        return RolResponse.from(saved, true);
    }

    @Transactional
    public void desactivarRolInstitucional(UUID idRol) {
        UUID idInstitucion = requireInstitutionTenant();
        Rol rol = rolRepository.findByIdAndIdInstitucion(idRol, idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Rol institucional no encontrado: " + idRol));
        rol.setEstado("INACTIVO");
        rolRepository.save(rol);
        auditoriaService.registrar(idInstitucion, SecurityUtils.currentUserId(),
                "ROL", "DESACTIVAR", "rol", rol.getId().toString(), true,
                "Rol institucional desactivado: " + rol.getNombre());
    }

    @Transactional(readOnly = true)
    public Rol resolveAssignableRole(UUID idRol, String codigoRol) {
        if (idRol == null && (codigoRol == null || codigoRol.isBlank())) {
            throw new IllegalArgumentException("Debes indicar un rol por id o por código");
        }

        Rol rol = idRol != null
                ? rolRepository.findById(idRol).orElseThrow(() -> new EntityNotFoundException("Rol no encontrado: " + idRol))
                : rolRepository.findByCodigo(codigoRol).orElseThrow(() -> new EntityNotFoundException("Rol no encontrado: " + codigoRol));

        if (SecurityUtils.currentUserHasRole("SUPER_ADMIN")) {
            return rol;
        }

        UUID idInstitucion = requireInstitutionTenant();
        if (rol.isEsGlobal() && "SUPER_ADMIN".equals(rol.getCodigo())) {
            throw new AccessDeniedException("No puedes asignar el rol SUPER_ADMIN desde una institución");
        }
        if (rol.getIdInstitucion() != null && !idInstitucion.equals(rol.getIdInstitucion())) {
            throw new AccessDeniedException("No puedes asignar un rol de otra institución");
        }
        return rol;
    }

    private Set<Permiso> loadPermisos(List<UUID> idsPermiso) {
        List<Permiso> permisos = permisoRepository.findAllByIdIn(idsPermiso);
        if (permisos.size() != idsPermiso.size()) {
            throw new IllegalArgumentException("Uno o más permisos no existen");
        }
        return Set.copyOf(permisos);
    }

    private List<RolResponse> buildInstitutionRoleList(UUID idInstitucion) {
        List<RolResponse> globales = rolRepository.findAllByEsGlobalTrueAndEstadoOrderByNombreAsc("ACTIVO").stream()
                .map(rol -> RolResponse.from(rol, false))
                .toList();
        List<RolResponse> institucionales = rolRepository.findAllByIdInstitucionAndEstadoOrderByNombreAsc(idInstitucion, "ACTIVO").stream()
                .map(rol -> RolResponse.from(rol, true))
                .toList();

        return java.util.stream.Stream.concat(globales.stream(), institucionales.stream()).toList();
    }

    private UUID requireInstitutionTenant() {
        UUID idInstitucion = TenantContext.get();
        return idInstitucion != null ? idInstitucion : SecurityUtils.requireCurrentInstitutionId();
    }

    private String generateRoleCode(UUID idInstitucion, String roleName) {
        String slug = Normalizer.normalize(roleName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9]+", "_")
                .replaceAll("^_+|_+$", "")
                .toUpperCase();
        String suffix = idInstitucion.toString().substring(0, 8).toUpperCase();
        return "INST_" + suffix + "_" + (slug.isBlank() ? "ROL" : slug);
    }
}
