package com.uagrm.si2g2.usuario.application;

import com.uagrm.si2g2.auditoria.application.AuditoriaService;
import com.uagrm.si2g2.auth.domain.Rol;
import com.uagrm.si2g2.auth.domain.RolRepository;
import com.uagrm.si2g2.auth.domain.Usuario;
import com.uagrm.si2g2.auth.domain.UsuarioRepository;
import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.tenant.TenantContext;
import com.uagrm.si2g2.usuario.dto.ActualizarUsuarioRequest;
import com.uagrm.si2g2.usuario.dto.AsignarRolRequest;
import com.uagrm.si2g2.usuario.dto.UsuarioResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final AuditoriaService auditoriaService;

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        UUID idInstitucion = TenantContext.get();
        if (idInstitucion == null) {
            return usuarioRepository.findAll().stream()
                    .map(UsuarioResponse::from).collect(Collectors.toList());
        }
        return usuarioRepository.findAllByIdInstitucion(idInstitucion).stream()
                .map(UsuarioResponse::from).collect(Collectors.toList());
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
        Rol rol = rolRepository.findByCodigo(request.getCodigoRol())
                .orElseThrow(() -> new EntityNotFoundException("Rol no encontrado: " + request.getCodigoRol()));
        usuario.getRoles().clear();
        usuario.getRoles().add(rol);
        UsuarioResponse resp = UsuarioResponse.from(usuarioRepository.save(usuario));
        auditoriaService.registrar(usuario.getIdInstitucion(), SecurityUtils.currentUserId(),
                "USUARIO", "ASIGNAR_ROL", "usuario", id.toString(),
                true, "Rol asignado: " + request.getCodigoRol());
        return resp;
    }

    private Usuario buscarConAcceso(UUID id) {
        UUID idInstitucion = TenantContext.get();
        if (idInstitucion != null) {
            return usuarioRepository.findByIdAndIdInstitucion(id, idInstitucion)
                    .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + id));
        }
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + id));
    }
}
