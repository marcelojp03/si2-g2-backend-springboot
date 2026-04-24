package com.uagrm.si2g2.asignacion.application;

import com.uagrm.si2g2.auditoria.application.AuditoriaService;
import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.asignacion.domain.AsignacionDocente;
import com.uagrm.si2g2.asignacion.domain.AsignacionDocenteRepository;
import com.uagrm.si2g2.asignacion.dto.AsignacionDocenteRequest;
import com.uagrm.si2g2.asignacion.dto.AsignacionDocenteResponse;
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
public class AsignacionDocenteService {

    private final AsignacionDocenteRepository repository;
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
        AsignacionDocenteResponse resp = AsignacionDocenteResponse.from(repository.save(a));
        auditoriaService.registrar(idInstitucion, SecurityUtils.currentUserId(),
                "ASIGNACION", "ASIGNAR", "asignacion_docente", resp.getId().toString(),
                true, "Docente asignado al paralelo: " + request.getIdParalelo());
        return resp;
    }

    @Transactional(readOnly = true)
    public List<AsignacionDocenteResponse> listar(UUID idDocente, UUID idGestion, UUID idParalelo) {
        UUID idInstitucion = TenantContext.get();
        if (idDocente != null) {
            return repository.findAllByIdInstitucionAndIdDocente(idInstitucion, idDocente)
                    .stream().map(AsignacionDocenteResponse::from).collect(Collectors.toList());
        }
        if (idParalelo != null) {
            return repository.findAllByIdInstitucionAndIdParalelo(idInstitucion, idParalelo)
                    .stream().map(AsignacionDocenteResponse::from).collect(Collectors.toList());
        }
        if (idGestion != null) {
            return repository.findAllByIdInstitucionAndIdGestion(idInstitucion, idGestion)
                    .stream().map(AsignacionDocenteResponse::from).collect(Collectors.toList());
        }
        return repository.findAllByIdInstitucion(idInstitucion)
                .stream().map(AsignacionDocenteResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AsignacionDocenteResponse obtener(UUID id) {
        return AsignacionDocenteResponse.from(buscar(id));
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
}
