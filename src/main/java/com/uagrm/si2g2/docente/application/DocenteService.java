package com.uagrm.si2g2.docente.application;

import com.uagrm.si2g2.auditoria.application.AuditoriaService;
import com.uagrm.si2g2.auth.domain.Usuario;
import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.docente.domain.Docente;
import com.uagrm.si2g2.docente.domain.DocenteRepository;
import com.uagrm.si2g2.docente.dto.DocenteRequest;
import com.uagrm.si2g2.docente.dto.DocenteResponse;
import com.uagrm.si2g2.materia.domain.Materia;
import com.uagrm.si2g2.materia.domain.MateriaRepository;
import com.uagrm.si2g2.persona.application.PersonaUsuarioSupport;
import com.uagrm.si2g2.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocenteService {

    private final DocenteRepository repository;
    private final MateriaRepository materiaRepository;
    private final PersonaUsuarioSupport personaUsuarioSupport;
    private final AuditoriaService auditoriaService;

    @Transactional
    public DocenteResponse crear(DocenteRequest request) {
        UUID idInstitucion = TenantContext.get();
        if (repository.existsByIdInstitucionAndCodigo(idInstitucion, request.getCodigo())) {
            throw new IllegalStateException("Ya existe un docente con el código: " + request.getCodigo());
        }

        Usuario usuario = personaUsuarioSupport.resolveOrCreate(
                idInstitucion,
                request.getCorreo(),
                "DOCENTE",
                request.getNombres(),
                request.getApellidos(),
                request.getTelefono(),
                request.getDocumentoIdentidad()
        );

        if (repository.findByIdUsuarioAndIdInstitucion(usuario.getId(), idInstitucion).isPresent()) {
            throw new IllegalStateException("Ya existe un perfil docente vinculado a este usuario");
        }

        Set<Materia> materias = loadMaterias(idInstitucion, request.getIdsMateria());
        Docente d = Docente.builder()
                .idInstitucion(idInstitucion)
                .idUsuario(usuario.getId())
                .codigo(request.getCodigo())
                .documentoIdentidad(request.getDocumentoIdentidad())
                .nombres(request.getNombres())
                .apellidos(request.getApellidos())
                .telefono(request.getTelefono())
                .correo(request.getCorreo())
                .materias(materias)
                .especialidad(formatEspecialidad(materias))
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
                .map(DocenteResponse::from)
                .collect(Collectors.toList());
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
        Set<Materia> materias = loadMaterias(idInstitucion, request.getIdsMateria());
        d.setCodigo(request.getCodigo());
        d.setDocumentoIdentidad(request.getDocumentoIdentidad());
        d.setNombres(request.getNombres());
        d.setApellidos(request.getApellidos());
        d.setTelefono(request.getTelefono());
        d.setCorreo(request.getCorreo());
        d.setMaterias(materias);
        d.setEspecialidad(formatEspecialidad(materias));
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

    private Set<Materia> loadMaterias(UUID idInstitucion, List<UUID> idsMateria) {
        if (idsMateria == null || idsMateria.isEmpty()) {
            return new HashSet<>();
        }
        List<Materia> materias = materiaRepository.findAllByIdInAndIdInstitucionAndEstado(idsMateria, idInstitucion, "ACTIVO");
        if (materias.size() != idsMateria.size()) {
            throw new IllegalArgumentException("Una o más materias no existen o no pertenecen a la institución");
        }
        return new HashSet<>(materias);
    }

    private static String formatEspecialidad(Set<Materia> materias) {
        if (materias == null || materias.isEmpty()) {
            return null;
        }
        return materias.stream()
                .map(Materia::getNombre)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.joining(", "));
    }
}
