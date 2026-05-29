package com.uagrm.si2g2.tutor.application;

import com.uagrm.si2g2.auditoria.application.AuditoriaService;
import com.uagrm.si2g2.auth.domain.Usuario;
import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.persona.application.PersonaUsuarioSupport;
import com.uagrm.si2g2.tenant.TenantContext;
import com.uagrm.si2g2.tutor.domain.Tutor;
import com.uagrm.si2g2.tutor.domain.TutorRepository;
import com.uagrm.si2g2.tutor.dto.TutorRequest;
import com.uagrm.si2g2.tutor.dto.TutorResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TutorService {

    private final TutorRepository repository;
    private final PersonaUsuarioSupport personaUsuarioSupport;
    private final AuditoriaService auditoriaService;

    @Transactional
    public TutorResponse crear(TutorRequest request) {
        UUID idInstitucion = TenantContext.get();

        Usuario usuario = personaUsuarioSupport.resolveOrCreate(
                idInstitucion,
                request.getCorreo(),
                "TUTOR",
                request.getNombres(),
                request.getApellidos(),
                request.getTelefono(),
                request.getDocumentoIdentidad()
        );

        if (repository.findByIdUsuarioAndIdInstitucion(usuario.getId(), idInstitucion).isPresent()) {
            throw new IllegalStateException("Ya existe un perfil tutor vinculado a este usuario");
        }

        Tutor t = Tutor.builder()
                .idInstitucion(idInstitucion)
                .idUsuario(usuario.getId())
                .documentoIdentidad(request.getDocumentoIdentidad())
                .nombres(request.getNombres())
                .apellidos(request.getApellidos())
                .telefono(request.getTelefono())
                .correo(request.getCorreo())
                .direccion(request.getDireccion())
                .build();
        TutorResponse resp = TutorResponse.from(repository.save(t));
        auditoriaService.registrar(idInstitucion, SecurityUtils.currentUserId(),
                "TUTOR", "CREAR", "tutor", resp.getId().toString(),
                true, "Tutor creado: " + t.getNombres() + " " + t.getApellidos());
        return resp;
    }

    @Transactional(readOnly = true)
    public List<TutorResponse> listar() {
        return repository.findAllByIdInstitucion(TenantContext.get()).stream()
                .map(TutorResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TutorResponse obtener(UUID id) {
        return TutorResponse.from(buscar(id));
    }

    @Transactional
    public TutorResponse actualizar(UUID id, TutorRequest request) {
        Tutor t = buscar(id);
        t.setDocumentoIdentidad(request.getDocumentoIdentidad());
        t.setNombres(request.getNombres());
        t.setApellidos(request.getApellidos());
        t.setTelefono(request.getTelefono());
        t.setCorreo(request.getCorreo());
        t.setDireccion(request.getDireccion());
        TutorResponse resp = TutorResponse.from(repository.save(t));
        auditoriaService.registrar(TenantContext.get(), SecurityUtils.currentUserId(),
                "TUTOR", "ACTUALIZAR", "tutor", id.toString(),
                true, "Tutor actualizado: " + t.getNombres() + " " + t.getApellidos());
        return resp;
    }

    @Transactional
    public void eliminar(UUID id) {
        Tutor t = buscar(id);
        t.setEstado("INACTIVO");
        repository.save(t);
        auditoriaService.registrar(TenantContext.get(), SecurityUtils.currentUserId(),
                "TUTOR", "ELIMINAR", "tutor", id.toString(),
                true, "Tutor desactivado: " + t.getNombres() + " " + t.getApellidos());
    }

    private Tutor buscar(UUID id) {
        return repository.findByIdAndIdInstitucion(id, TenantContext.get())
                .orElseThrow(() -> new EntityNotFoundException("Tutor no encontrado: " + id));
    }
}
