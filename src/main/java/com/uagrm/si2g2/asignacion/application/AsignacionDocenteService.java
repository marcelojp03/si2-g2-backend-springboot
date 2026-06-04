package com.uagrm.si2g2.asignacion.application;

import com.uagrm.si2g2.auditoria.application.AuditoriaService;
import com.uagrm.si2g2.academico.domain.GestionAcademica;
import com.uagrm.si2g2.academico.domain.GestionAcademicaRepository;
import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.asignacion.domain.AsignacionDocente;
import com.uagrm.si2g2.asignacion.domain.AsignacionDocenteRepository;
import com.uagrm.si2g2.asignacion.dto.AsignacionDocenteRequest;
import com.uagrm.si2g2.asignacion.dto.AsignacionDocenteResponse;
import com.uagrm.si2g2.curso.domain.Paralelo;
import com.uagrm.si2g2.curso.domain.ParaleloRepository;
import com.uagrm.si2g2.docente.domain.Docente;
import com.uagrm.si2g2.docente.domain.DocenteRepository;
import com.uagrm.si2g2.materia.domain.Materia;
import com.uagrm.si2g2.materia.domain.MateriaRepository;
import com.uagrm.si2g2.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AsignacionDocenteService {

    private final AsignacionDocenteRepository repository;
    private final DocenteRepository docenteRepository;
    private final MateriaRepository materiaRepository;
    private final ParaleloRepository paraleloRepository;
    private final GestionAcademicaRepository gestionRepository;
    private final AuditoriaService auditoriaService;

    @Transactional
    public AsignacionDocenteResponse asignar(AsignacionDocenteRequest request) {
        UUID idInstitucion = TenantContext.get();
        if (repository.existsByIdInstitucionAndIdDocenteAndIdMateriaAndIdParaleloAndIdGestion(
                idInstitucion, request.getIdDocente(), request.getIdMateria(),
                request.getIdParalelo(), request.getIdGestion())) {
            throw new IllegalStateException(
                    "El docente ya tiene esa asignación en el paralelo y gestión indicados");
        }
        AsignacionDocente a = AsignacionDocente.builder()
                .idInstitucion(idInstitucion)
                .idDocente(request.getIdDocente())
                .idMateria(request.getIdMateria())
                .idParalelo(request.getIdParalelo())
                .idGestion(request.getIdGestion())
                .build();
        AsignacionDocenteResponse resp = toResponse(repository.save(a));
        auditoriaService.registrar(idInstitucion, SecurityUtils.currentUserId(),
                "ASIGNACION", "ASIGNAR", "asignacion_docente", resp.getId().toString(),
                true, "Docente asignado al paralelo: " + request.getIdParalelo());
        return resp;
    }

    @Transactional(readOnly = true)
    public List<AsignacionDocenteResponse> listar(UUID idDocente, UUID idGestion, UUID idParalelo) {
        UUID idInstitucion = TenantContext.get();
        List<AsignacionDocente> asignaciones;
        if (idDocente != null) {
            asignaciones = repository.findAllByIdInstitucionAndIdDocente(idInstitucion, idDocente);
        } else if (idParalelo != null) {
            asignaciones = repository.findAllByIdInstitucionAndIdParalelo(idInstitucion, idParalelo);
        } else if (idGestion != null) {
            asignaciones = repository.findAllByIdInstitucionAndIdGestion(idInstitucion, idGestion);
        } else {
            asignaciones = repository.findAllByIdInstitucion(idInstitucion);
        }
        return toResponses(asignaciones, idInstitucion);
    }

    @Transactional(readOnly = true)
    public AsignacionDocenteResponse obtener(UUID id) {
        return toResponse(buscar(id));
    }

    @Transactional
    public void eliminar(UUID id) {
        AsignacionDocente a = buscar(id);
        a.setEstado("INACTIVA");
        repository.save(a);
        auditoriaService.registrar(TenantContext.get(), SecurityUtils.currentUserId(),
                "ASIGNACION", "ELIMINAR", "asignacion_docente", id.toString(),
                true, "Asignación desactivada");
    }

    private AsignacionDocente buscar(UUID id) {
        return repository.findByIdAndIdInstitucion(id, TenantContext.get())
                .orElseThrow(() -> new EntityNotFoundException("Asignación no encontrada: " + id));
    }

    private AsignacionDocenteResponse toResponse(AsignacionDocente asignacion) {
        return toResponses(List.of(asignacion), asignacion.getIdInstitucion()).getFirst();
    }

    private List<AsignacionDocenteResponse> toResponses(List<AsignacionDocente> asignaciones, UUID idInstitucion) {
        Map<UUID, String> docentes = docenteRepository.findAllById(asignaciones.stream().map(AsignacionDocente::getIdDocente).collect(Collectors.toSet()))
                .stream()
                .filter(d -> idInstitucion.equals(d.getIdInstitucion()))
                .collect(Collectors.toMap(Docente::getId, d -> d.getApellidos() + ", " + d.getNombres()));
        Map<UUID, String> materias = materiaRepository.findAllById(asignaciones.stream().map(AsignacionDocente::getIdMateria).collect(Collectors.toSet()))
                .stream()
                .filter(m -> idInstitucion.equals(m.getIdInstitucion()))
                .collect(Collectors.toMap(Materia::getId, Materia::getNombre));
        Map<UUID, String> paralelos = paraleloRepository.findAllById(asignaciones.stream().map(AsignacionDocente::getIdParalelo).collect(Collectors.toSet()))
                .stream()
                .filter(p -> idInstitucion.equals(p.getIdInstitucion()))
                .collect(Collectors.toMap(Paralelo::getId, Paralelo::getNombre));
        Map<UUID, String> gestiones = gestionRepository.findAllById(asignaciones.stream().map(AsignacionDocente::getIdGestion).collect(Collectors.toSet()))
                .stream()
                .filter(g -> idInstitucion.equals(g.getIdInstitucion()))
                .collect(Collectors.toMap(GestionAcademica::getId, GestionAcademica::getNombre));

        return asignaciones.stream()
                .map(a -> AsignacionDocenteResponse.from(
                        a,
                        docentes.get(a.getIdDocente()),
                        materias.get(a.getIdMateria()),
                        paralelos.get(a.getIdParalelo()),
                        gestiones.get(a.getIdGestion())))
                .collect(Collectors.toList());
    }
}
