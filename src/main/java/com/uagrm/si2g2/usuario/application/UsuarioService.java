package com.uagrm.si2g2.usuario.application;

import com.uagrm.si2g2.auditoria.application.AuditoriaService;
import com.uagrm.si2g2.auth.domain.Rol;
import com.uagrm.si2g2.auth.domain.Usuario;
import com.uagrm.si2g2.auth.domain.UsuarioRepository;
import com.uagrm.si2g2.auth.application.RoleService;
import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.persona.application.PersonaProvisioningService;
import com.uagrm.si2g2.tenant.TenantContext;
import com.uagrm.si2g2.usuario.dto.ActualizarUsuarioRequest;
import com.uagrm.si2g2.usuario.dto.AsignarRolRequest;
import com.uagrm.si2g2.usuario.dto.PaginatedUsuarioResponse;
import com.uagrm.si2g2.usuario.dto.UsuarioResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RoleService roleService;
    private final PersonaProvisioningService personaProvisioningService;
    private final AuditoriaService auditoriaService;

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        UUID idInstitucion = TenantContext.get();
        if (idInstitucion == null) {
            if (!SecurityUtils.currentUserHasRole("SUPER_ADMIN")) {
                throw new AccessDeniedException("Solo SUPER_ADMIN puede listar usuarios globalmente");
            }
            return usuarioRepository.findAll().stream()
                    .map(UsuarioResponse::from).collect(Collectors.toList());
        }
        return usuarioRepository.findAllByIdInstitucion(idInstitucion).stream()
                .map(UsuarioResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PaginatedUsuarioResponse listarPaginado(String search, int page, int size, String sortField, String sortDir) {
        UUID idInstitucion = TenantContext.get();
        if (idInstitucion == null && !SecurityUtils.currentUserHasRole("SUPER_ADMIN")) {
            throw new AccessDeniedException("Solo SUPER_ADMIN puede listar usuarios globalmente");
        }

        Sort sort = Sort.by(sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC,
                sortField != null ? sortField : "correo");
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Usuario> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (idInstitucion != null) {
                predicates.add(cb.equal(root.get("idInstitucion"), idInstitucion));
            }
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("correo")), pattern),
                        cb.like(cb.lower(root.get("nombres")), pattern),
                        cb.like(cb.lower(root.get("apellidos")), pattern)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Usuario> pageResult = usuarioRepository.findAll(spec, pageable);

        return PaginatedUsuarioResponse.builder()
                .usuarios(pageResult.getContent().stream().map(UsuarioResponse::from).toList())
                .total(pageResult.getTotalElements())
                .pagina(pageResult.getNumber())
                .totalPaginas(pageResult.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(UUID id) {
        Usuario usuario = buscarConAcceso(id);
        return UsuarioResponse.from(usuario);
    }

    @Transactional
    public UsuarioResponse actualizar(UUID id, ActualizarUsuarioRequest request) {
        Usuario usuario = buscarConAcceso(id);
        usuario.setNombres(request.getNombres());
        usuario.setApellidos(request.getApellidos());
        usuario.setTelefono(request.getTelefono());
        UsuarioResponse resp = UsuarioResponse.from(usuarioRepository.save(usuario));
        auditoriaService.registrar(usuario.getIdInstitucion(), SecurityUtils.currentUserId(),
                "USUARIO", "ACTUALIZAR", "usuario", id.toString(),
                true, "Usuario actualizado: " + usuario.getCorreo());
        return resp;
    }

    @Transactional
    public UsuarioResponse desactivar(UUID id) {
        Usuario usuario = buscarConAcceso(id);
        usuario.setEstado("INACTIVO");
        UsuarioResponse resp = UsuarioResponse.from(usuarioRepository.save(usuario));
        auditoriaService.registrar(usuario.getIdInstitucion(), SecurityUtils.currentUserId(),
                "USUARIO", "DESACTIVAR", "usuario", id.toString(),
                true, "Usuario desactivado: " + usuario.getCorreo());
        return resp;
    }

    @Transactional
    public UsuarioResponse asignarRol(UUID id, AsignarRolRequest request) {
        Usuario usuario = buscarConAcceso(id);
        Rol rol = roleService.resolveAssignableRole(request.getIdRol(), request.getCodigoRol());
        usuario.getRoles().clear();
        usuario.getRoles().add(rol);
        UsuarioResponse resp = UsuarioResponse.from(usuarioRepository.save(usuario));
        personaProvisioningService.provisionForUsuario(usuario);
        auditoriaService.registrar(usuario.getIdInstitucion(), SecurityUtils.currentUserId(),
                "USUARIO", "ASIGNAR_ROL", "usuario", id.toString(),
                true, "Rol asignado: " + rol.getCodigo());
        return resp;
    }

    private Usuario buscarConAcceso(UUID id) {
        UUID idInstitucion = TenantContext.get();
        if (idInstitucion != null) {
            return usuarioRepository.findByIdAndIdInstitucion(id, idInstitucion)
                    .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + id));
        }

        if (!SecurityUtils.currentUserHasRole("SUPER_ADMIN")) {
            throw new AccessDeniedException("No tienes permisos para acceder a usuarios fuera de tu institución");
        }

        return usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + id));
    }
}
