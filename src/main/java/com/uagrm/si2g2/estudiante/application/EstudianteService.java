package com.uagrm.si2g2.estudiante.application;

import com.uagrm.si2g2.auditoria.application.AuditoriaService;
import com.uagrm.si2g2.auth.domain.Usuario;
import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.estudiante.domain.Estudiante;
import com.uagrm.si2g2.estudiante.domain.EstudianteRepository;
import com.uagrm.si2g2.estudiante.dto.EstudianteRequest;
import com.uagrm.si2g2.estudiante.dto.EstudianteResponse;
import com.uagrm.si2g2.persona.application.PersonaUsuarioSupport;
import com.uagrm.si2g2.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EstudianteService {

    private final EstudianteRepository repository;
    private final PersonaUsuarioSupport personaUsuarioSupport;
    private final AuditoriaService auditoriaService;

    @Transactional
    public EstudianteResponse crear(EstudianteRequest request) {
        UUID idInstitucion = TenantContext.get();
        if (repository.existsByIdInstitucionAndCodigoEstudiante(idInstitucion, request.getCodigoEstudiante())) {
            throw new IllegalStateException("Ya existe un estudiante con el código: " + request.getCodigoEstudiante());
        }

        Usuario usuario = personaUsuarioSupport.resolveOrCreate(
                idInstitucion,
                request.getCorreo(),
                "ESTUDIANTE",
                request.getNombres(),
                request.getApellidos(),
                request.getTelefono(),
                request.getDocumentoIdentidad()
        );

        if (repository.findByIdUsuarioAndIdInstitucion(usuario.getId(), idInstitucion).isPresent()) {
            throw new IllegalStateException("Ya existe un perfil estudiante vinculado a este usuario");
        }

        Estudiante e = Estudiante.builder()
                .idInstitucion(idInstitucion)
                .idUsuario(usuario.getId())
                .codigoEstudiante(request.getCodigoEstudiante())
                .documentoIdentidad(request.getDocumentoIdentidad())
                .nombres(request.getNombres())
                .apellidos(request.getApellidos())
                .fechaNacimiento(request.getFechaNacimiento())
                .sexo(request.getSexo())
                .direccion(request.getDireccion())
                .telefono(request.getTelefono())
                .correo(request.getCorreo())
                .build();
        EstudianteResponse resp = EstudianteResponse.from(repository.save(e));
        auditoriaService.registrar(idInstitucion, SecurityUtils.currentUserId(),
                "ESTUDIANTE", "CREAR", "estudiante", resp.getId().toString(),
                true, "Estudiante creado: " + resp.getCodigoEstudiante());
        return resp;
    }

    @Transactional(readOnly = true)
    public List<EstudianteResponse> listar() {
        return repository.findAllByIdInstitucion(TenantContext.get()).stream()
                .map(EstudianteResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EstudianteResponse obtener(UUID id) {
        return EstudianteResponse.from(buscar(id));
    }

    @Transactional
    public EstudianteResponse actualizar(UUID id, EstudianteRequest request) {
        UUID idInstitucion = TenantContext.get();
        Estudiante e = buscar(id);
        if (!e.getCodigoEstudiante().equals(request.getCodigoEstudiante())
                && repository.existsByIdInstitucionAndCodigoEstudiante(idInstitucion, request.getCodigoEstudiante())) {
            throw new IllegalStateException("Ya existe un estudiante con el código: " + request.getCodigoEstudiante());
        }
        e.setCodigoEstudiante(request.getCodigoEstudiante());
        e.setDocumentoIdentidad(request.getDocumentoIdentidad());
        e.setNombres(request.getNombres());
        e.setApellidos(request.getApellidos());
        e.setFechaNacimiento(request.getFechaNacimiento());
        e.setSexo(request.getSexo());
        e.setDireccion(request.getDireccion());
        e.setTelefono(request.getTelefono());
        e.setCorreo(request.getCorreo());
        EstudianteResponse resp = EstudianteResponse.from(repository.save(e));
        auditoriaService.registrar(TenantContext.get(), SecurityUtils.currentUserId(),
                "ESTUDIANTE", "ACTUALIZAR", "estudiante", id.toString(),
                true, "Estudiante actualizado: " + resp.getCodigoEstudiante());
        return resp;
    }

    @Transactional
    public void eliminar(UUID id) {
        Estudiante e = buscar(id);
        e.setEstado("INACTIVO");
        repository.save(e);
        auditoriaService.registrar(TenantContext.get(), SecurityUtils.currentUserId(),
                "ESTUDIANTE", "ELIMINAR", "estudiante", id.toString(),
                true, "Estudiante desactivado: " + e.getCodigoEstudiante());
    }

    private Estudiante buscar(UUID id) {
        return repository.findByIdAndIdInstitucion(id, TenantContext.get())
                .orElseThrow(() -> new EntityNotFoundException("Estudiante no encontrado: " + id));
    }
}
