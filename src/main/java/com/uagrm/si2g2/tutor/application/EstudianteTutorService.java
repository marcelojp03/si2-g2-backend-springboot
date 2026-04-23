package com.uagrm.si2g2.tutor.application;

import com.uagrm.si2g2.tutor.domain.EstudianteTutor;
import com.uagrm.si2g2.tutor.domain.EstudianteTutorRepository;
import com.uagrm.si2g2.tutor.dto.EstudianteTutorRequest;
import com.uagrm.si2g2.tutor.dto.EstudianteTutorResponse;
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
public class EstudianteTutorService {

    private final EstudianteTutorRepository repository;

    @Transactional
    public EstudianteTutorResponse vincular(UUID idEstudiante, EstudianteTutorRequest request) {
        UUID idInstitucion = TenantContext.get();
        if (repository.existsByIdInstitucionAndIdEstudianteAndIdTutor(
                idInstitucion, idEstudiante, request.getIdTutor())) {
            throw new IllegalStateException("El tutor ya está vinculado a este estudiante");
        }
        if (request.isEsPrincipal()) {
            // Solo puede haber un tutor principal activo por estudiante
            repository.desmarcarPrincipalPorEstudiante(idInstitucion, idEstudiante);
        }
        EstudianteTutor et = EstudianteTutor.builder()
                .idInstitucion(idInstitucion)
                .idEstudiante(idEstudiante)
                .idTutor(request.getIdTutor())
                .parentesco(request.getParentesco())
                .esPrincipal(request.isEsPrincipal())
                .build();
        return EstudianteTutorResponse.from(repository.save(et));
    }

    @Transactional(readOnly = true)
    public List<EstudianteTutorResponse> listarPorEstudiante(UUID idEstudiante) {
        return repository.findAllByIdInstitucionAndIdEstudiante(TenantContext.get(), idEstudiante).stream()
                .map(EstudianteTutorResponse::from).collect(Collectors.toList());
    }

    @Transactional
    public void desvincular(UUID idEstudiante, UUID idTutor) {
        UUID idInstitucion = TenantContext.get();
        EstudianteTutor et = repository.findByIdInstitucionAndIdEstudianteAndIdTutor(
                        idInstitucion, idEstudiante, idTutor)
                .orElseThrow(() -> new EntityNotFoundException("Vínculo no encontrado"));
        et.setEstado("INACTIVO");
        repository.save(et);
    }
}
