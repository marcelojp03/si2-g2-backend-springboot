package com.uagrm.si2g2.inscripcion.application;

import com.uagrm.si2g2.inscripcion.domain.Inscripcion;
import com.uagrm.si2g2.inscripcion.domain.InscripcionRepository;
import com.uagrm.si2g2.inscripcion.dto.InscripcionRequest;
import com.uagrm.si2g2.inscripcion.dto.InscripcionResponse;
import com.uagrm.si2g2.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InscripcionService {

    private final InscripcionRepository repository;

    @Transactional
    public InscripcionResponse inscribir(InscripcionRequest request) {
        UUID idInstitucion = TenantContext.get();
        if (repository.existsByIdInstitucionAndIdEstudianteAndIdGestionAndEstado(
                idInstitucion, request.getIdEstudiante(), request.getIdGestion(), "ACTIVO")) {
            throw new IllegalStateException(
                    "El estudiante ya tiene una inscripción activa en esta gestión");
        }
        LocalDate fecha = request.getFechaInscripcion() != null
                ? request.getFechaInscripcion()
                : LocalDate.now();
        Inscripcion i = Inscripcion.builder()
                .idInstitucion(idInstitucion)
                .idEstudiante(request.getIdEstudiante())
                .idGestion(request.getIdGestion())
                .idCurso(request.getIdCurso())
                .idParalelo(request.getIdParalelo())
                .fechaInscripcion(fecha)
                .build();
        return InscripcionResponse.from(repository.save(i));
    }

    @Transactional(readOnly = true)
    public List<InscripcionResponse> listar(UUID idEstudiante, UUID idGestion, UUID idParalelo) {
        UUID idInstitucion = TenantContext.get();
        if (idEstudiante != null) {
            return repository.findAllByIdInstitucionAndIdEstudiante(idInstitucion, idEstudiante)
                    .stream().map(InscripcionResponse::from).collect(Collectors.toList());
        }
        if (idParalelo != null) {
            return repository.findAllByIdInstitucionAndIdParalelo(idInstitucion, idParalelo)
                    .stream().map(InscripcionResponse::from).collect(Collectors.toList());
        }
        if (idGestion != null) {
            return repository.findAllByIdInstitucionAndIdGestion(idInstitucion, idGestion)
                    .stream().map(InscripcionResponse::from).collect(Collectors.toList());
        }
        return repository.findAllByIdInstitucion(idInstitucion)
                .stream().map(InscripcionResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InscripcionResponse obtener(UUID id) {
        return InscripcionResponse.from(buscar(id));
    }

    @Transactional
    public void anular(UUID id) {
        Inscripcion i = buscar(id);
        i.setEstado("ANULADO");
        repository.save(i);
    }

    private Inscripcion buscar(UUID id) {
        return repository.findByIdAndIdInstitucion(id, TenantContext.get())
                .orElseThrow(() -> new EntityNotFoundException("Inscripción no encontrada: " + id));
    }
}
