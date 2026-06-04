package com.uagrm.si2g2.auth.application;

import com.uagrm.si2g2.auditoria.application.AuditoriaService;
import com.uagrm.si2g2.auth.domain.Permiso;
import com.uagrm.si2g2.auth.domain.PermisoRepository;
import com.uagrm.si2g2.auth.domain.Rol;
import com.uagrm.si2g2.auth.domain.RolRepository;
import com.uagrm.si2g2.auth.domain.Usuario;
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
                    .map(rol -> RolResponse.from(rol, isEditableBySuperAdmin(rol)))
                    .toList();
        }

        UUID idInstitucion = requireInstitutionTenant();
        return buildInstitutionRoleList(idInstitucion);
    }

    @Transactional(readOnly = true)
    public List<RolResponse> listarRolesAsignables() {
        Usuario assigner = SecurityUtils.currentUser();
        if (assigner == null) {
            throw new AccessDeniedException("Usuario no autenticado");
        }

        List<Rol> roles;
        if (SecurityUtils.currentUserHasRole("SUPER_ADMIN")) {
            roles = rolRepository.findAll().stream()
                    .filter(rol -> "ACTIVO".equals(rol.getEstado()))
                    .filter(rol -> !"SUPER_ADMIN".equals(rol.getCodigo()))
                    .toList();
        } else {
            UUID idInstitucion = requireInstitutionTenant();
            roles = java.util.stream.Stream.concat(
                    rolRepository.findAllByEsGlobalTrueAndEstadoOrderByNombreAsc("ACTIVO").stream(),
                    rolRepository.findAllByIdInstitucionAndEstadoOrderByNombreAsc(idInstitucion, "ACTIVO").stream()
            ).toList();
        }

        return roles.stream()
                .filter(rol -> RoleHierarchy.canAssign(assigner, rol))
                .map(rol -> RolResponse.from(rol, false))
                .toList();
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
        if (SecurityUtils.currentUserHasRole("SUPER_ADMIN")) {
            return actualizarComoSuperAdmin(idRol, request);
        }

        UUID idInstitucion = requireInstitutionTenant();
        Rol rol = rolRepository.findById(idRol)
                .orElseThrow(() -> new EntityNotFoundException("Rol no encontrado: " + idRol));

        if (rol.isEsGlobal()) {
            if (!SecurityUtils.currentUserHasAuthority("ROLES_UPDATE")) {
                throw new AccessDeniedException("No tienes permiso para modificar roles predefinidos");
            }
            if ("SUPER_ADMIN".equals(rol.getCodigo())) {
                throw new AccessDeniedException("El rol SUPER_ADMIN no puede modificarse");
            }
            rol.setDescripcion(request.getDescripcion());
            rol.setPermisos(loadPermisos(request.getIdsPermiso()));
            Rol saved = rolRepository.save(rol);
            auditoriaService.registrar(idInstitucion, SecurityUtils.currentUserId(),
                    "ROL", "ACTUALIZAR", "rol", saved.getId().toString(), true,
                    "Permisos de rol predefinido actualizados: " + saved.getCodigo());
            return RolResponse.from(saved, isEditableGlobalRole(saved, true));
        }

        rol = rolRepository.findByIdAndIdInstitucion(idRol, idInstitucion)
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
        if (SecurityUtils.currentUserHasRole("SUPER_ADMIN")) {
            desactivarComoSuperAdmin(idRol);
            return;
        }

        UUID idInstitucion = requireInstitutionTenant();
        Rol rol = rolRepository.findById(idRol)
                .orElseThrow(() -> new EntityNotFoundException("Rol no encontrado: " + idRol));

        if (rol.isEsGlobal()) {
            throw new AccessDeniedException("Los roles predefinidos del sistema no pueden desactivarse desde la institución");
        }

        rol = rolRepository.findByIdAndIdInstitucion(idRol, idInstitucion)
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
        if (!RoleHierarchy.canAssign(SecurityUtils.currentUser(), rol)) {
            throw new AccessDeniedException("No tienes permiso para asignar el rol: " + rol.getCodigo());
        }
        return rol;
    }

    private RolResponse actualizarComoSuperAdmin(UUID idRol, RolRequest request) {
        Rol rol = rolRepository.findById(idRol)
                .orElseThrow(() -> new EntityNotFoundException("Rol no encontrado: " + idRol));

        if ("SUPER_ADMIN".equals(rol.getCodigo())) {
            throw new AccessDeniedException("El rol SUPER_ADMIN no puede modificarse");
        }

        if (rol.isEsGlobal()) {
            rol.setDescripcion(request.getDescripcion());
            rol.setPermisos(loadPermisos(request.getIdsPermiso()));
        } else {
            UUID idInstitucion = rol.getIdInstitucion();
            if (idInstitucion == null) {
                throw new IllegalStateException("Rol institucional sin institución asociada");
            }
            if (!rol.getNombre().equalsIgnoreCase(request.getNombre())
                    && rolRepository.existsByIdInstitucionAndNombreIgnoreCase(idInstitucion, request.getNombre())) {
                throw new IllegalStateException("Ya existe un rol institucional con el nombre: " + request.getNombre());
            }
            rol.setNombre(request.getNombre().trim());
            rol.setDescripcion(request.getDescripcion());
            rol.setPermisos(loadPermisos(request.getIdsPermiso()));
        }

        Rol saved = rolRepository.save(rol);
        auditoriaService.registrar(saved.getIdInstitucion(), SecurityUtils.currentUserId(),
                "ROL", "ACTUALIZAR", "rol", saved.getId().toString(), true,
                "Rol actualizado por SUPER_ADMIN: " + saved.getCodigo());
        return RolResponse.from(saved, isEditableBySuperAdmin(saved));
    }

    private void desactivarComoSuperAdmin(UUID idRol) {
        Rol rol = rolRepository.findById(idRol)
                .orElseThrow(() -> new EntityNotFoundException("Rol no encontrado: " + idRol));

        if ("SUPER_ADMIN".equals(rol.getCodigo())) {
            throw new AccessDeniedException("El rol SUPER_ADMIN no puede desactivarse");
        }

        rol.setEstado("INACTIVO");
        rolRepository.save(rol);
        auditoriaService.registrar(rol.getIdInstitucion(), SecurityUtils.currentUserId(),
                "ROL", "DESACTIVAR", "rol", rol.getId().toString(), true,
                "Rol desactivado por SUPER_ADMIN: " + rol.getCodigo());
    }

    private static boolean isEditableBySuperAdmin(Rol rol) {
        return !"SUPER_ADMIN".equals(rol.getCodigo()) && "ACTIVO".equals(rol.getEstado());
    }

    private Set<Permiso> loadPermisos(List<UUID> idsPermiso) {
        List<Permiso> permisos = permisoRepository.findAllByIdIn(idsPermiso);
        if (permisos.size() != idsPermiso.size()) {
            throw new IllegalArgumentException("Uno o más permisos no existen");
        }
        return new java.util.HashSet<>(permisos);
    }

    private List<RolResponse> buildInstitutionRoleList(UUID idInstitucion) {
        boolean canWriteRoles = SecurityUtils.currentUserHasAuthority("ROLES_UPDATE");
        List<RolResponse> globales = rolRepository.findAllByEsGlobalTrueAndEstadoOrderByNombreAsc("ACTIVO").stream()
                .map(rol -> RolResponse.from(rol, isEditableGlobalRole(rol, canWriteRoles)))
                .toList();
        List<RolResponse> institucionales = rolRepository.findAllByIdInstitucionAndEstadoOrderByNombreAsc(idInstitucion, "ACTIVO").stream()
                .map(rol -> RolResponse.from(rol, canWriteRoles))
                .toList();

        return java.util.stream.Stream.concat(globales.stream(), institucionales.stream()).toList();
    }

    private static boolean isEditableGlobalRole(Rol rol) {
        return isEditableGlobalRole(rol, true);
    }

    private static boolean isEditableGlobalRole(Rol rol, boolean canWriteRoles) {
        return canWriteRoles
                && !"SUPER_ADMIN".equals(rol.getCodigo())
                && "ACTIVO".equals(rol.getEstado());
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
        String codigo = slug.isBlank() ? "ROL" : slug;

        String existing = rolRepository.findByCodigo(codigo).map(Rol::getCodigo).orElse(null);
        if (existing == null) return codigo;

        int counter = 2;
        while (rolRepository.findByCodigo(codigo + "_" + counter).isPresent()) counter++;
        return codigo + "_" + counter;
    }
}
