package com.uagrm.si2g2.usuario.application;

import com.uagrm.si2g2.auth.domain.Rol;
import com.uagrm.si2g2.auth.domain.RolRepository;
import com.uagrm.si2g2.auth.domain.Usuario;
import com.uagrm.si2g2.auth.domain.UsuarioRepository;
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
        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse desactivar(UUID id) {
        Usuario usuario = buscarConAcceso(id);
        usuario.setEstado("INACTIVO");
        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse asignarRol(UUID id, AsignarRolRequest request) {
        Usuario usuario = buscarConAcceso(id);
        Rol rol = rolRepository.findByCodigo(request.getCodigoRol())
                .orElseThrow(() -> new EntityNotFoundException("Rol no encontrado: " + request.getCodigoRol()));
        // Reemplaza todos los roles actuales por el nuevo (un solo rol activo)
        usuario.getRoles().clear();
        usuario.getRoles().add(rol);
        return UsuarioResponse.from(usuarioRepository.save(usuario));
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
