package com.uagrm.si2g2.docente.application;

import com.uagrm.si2g2.auditoria.application.AuditoriaService;
import com.uagrm.si2g2.auth.domain.Rol;
import com.uagrm.si2g2.auth.domain.RolRepository;
import com.uagrm.si2g2.auth.domain.Usuario;
import com.uagrm.si2g2.auth.domain.UsuarioRepository;
import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.docente.domain.Docente;
import com.uagrm.si2g2.docente.domain.DocenteRepository;
import com.uagrm.si2g2.docente.dto.DocenteRequest;
import com.uagrm.si2g2.docente.dto.DocenteResponse;
import com.uagrm.si2g2.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocenteService {

    private final DocenteRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;

    @Transactional
    public DocenteResponse crear(DocenteRequest request) {
        UUID idInstitucion = TenantContext.get();
        if (repository.existsByIdInstitucionAndCodigo(idInstitucion, request.getCodigo())) {
            throw new IllegalStateException("Ya existe un docente con el código: " + request.getCodigo());
        }
        if (usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new IllegalStateException("Ya existe un usuario con el correo: " + request.getCorreo());
        }
        Rol rol = rolRepository.findByCodigo("DOCENTE")
                .orElseThrow(() -> new IllegalStateException("Rol DOCENTE no encontrado"));
        Usuario usuario = Usuario.builder()
                .idInstitucion(idInstitucion)
                .correo(request.getCorreo())
                .hashContrasena(passwordEncoder.encode(request.getDocumentoIdentidad()))
                .nombres(request.getNombres())
                .apellidos(request.getApellidos())
                .telefono(request.getTelefono())
                .roles(Set.of(rol))
                .requiereCambioContrasena(false)
                .build();
        usuarioRepository.save(usuario);
        Docente d = Docente.builder()
                .idInstitucion(idInstitucion)
                .idUsuario(usuario.getId())
                .codigo(request.getCodigo())
                .documentoIdentidad(request.getDocumentoIdentidad())
                .nombres(request.getNombres())
                .apellidos(request.getApellidos())
                .telefono(request.getTelefono())
                .correo(request.getCorreo())
                .especialidad(request.getEspecialidad())
                .build();
        DocenteResponse resp = DocenteResponse.from(repository.save(d));
        auditoriaService.registrar(idInstitucion, SecurityUtils.currentUserId(),
                "DOCENTE", "CREAR", "docente", resp.getId().toString(),
                true, "Docente creado: " + resp.getCodigo());
        return resp;
    }

    @Transactional(readOnly = true)
    public List<DocenteResponse> listar() {
        return repository.findAllByIdInstitucion(TenantContext.get()).stream()
                .map(DocenteResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DocenteResponse obtener(UUID id) {
        return DocenteResponse.from(buscar(id));
    }

    @Transactional
    public DocenteResponse actualizar(UUID id, DocenteRequest request) {
        UUID idInstitucion = TenantContext.get();
        Docente d = buscar(id);
        if (!d.getCodigo().equals(request.getCodigo())
                && repository.existsByIdInstitucionAndCodigo(idInstitucion, request.getCodigo())) {
            throw new IllegalStateException("Ya existe un docente con el código: " + request.getCodigo());
        }
        d.setCodigo(request.getCodigo());
        d.setDocumentoIdentidad(request.getDocumentoIdentidad());
        d.setNombres(request.getNombres());
        d.setApellidos(request.getApellidos());
        d.setTelefono(request.getTelefono());
        d.setCorreo(request.getCorreo());
        d.setEspecialidad(request.getEspecialidad());
        DocenteResponse resp = DocenteResponse.from(repository.save(d));
        auditoriaService.registrar(TenantContext.get(), SecurityUtils.currentUserId(),
                "DOCENTE", "ACTUALIZAR", "docente", id.toString(),
                true, "Docente actualizado: " + resp.getCodigo());
        return resp;
    }

    @Transactional
    public void eliminar(UUID id) {
        Docente d = buscar(id);
        d.setEstado("INACTIVO");
        repository.save(d);
        auditoriaService.registrar(TenantContext.get(), SecurityUtils.currentUserId(),
                "DOCENTE", "ELIMINAR", "docente", id.toString(),
                true, "Docente desactivado: " + d.getCodigo());
    }

    private Docente buscar(UUID id) {
        return repository.findByIdAndIdInstitucion(id, TenantContext.get())
                .orElseThrow(() -> new EntityNotFoundException("Docente no encontrado: " + id));
    }
}
