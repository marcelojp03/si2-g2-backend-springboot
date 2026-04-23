package com.uagrm.si2g2.curso.application;

import com.uagrm.si2g2.curso.domain.CursoMateria;
import com.uagrm.si2g2.curso.domain.CursoMateriaRepository;
import com.uagrm.si2g2.curso.dto.CursoMateriaRequest;
import com.uagrm.si2g2.curso.dto.CursoMateriaResponse;
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
public class CursoMateriaService {

    private final CursoMateriaRepository repository;

    @Transactional
    public CursoMateriaResponse asignar(UUID idCurso, CursoMateriaRequest request) {
        UUID idInstitucion = TenantContext.get();
        if (repository.existsByIdInstitucionAndIdCursoAndIdMateria(
                idInstitucion, idCurso, request.getIdMateria())) {
            throw new IllegalStateException("La materia ya está asignada a este curso");
        }
        CursoMateria cm = CursoMateria.builder()
                .idInstitucion(idInstitucion)
                .idCurso(idCurso)
                .idMateria(request.getIdMateria())
                .build();
        return CursoMateriaResponse.from(repository.save(cm));
    }

    @Transactional(readOnly = true)
    public List<CursoMateriaResponse> listarPorCurso(UUID idCurso) {
        return repository.findAllByIdInstitucionAndIdCurso(TenantContext.get(), idCurso).stream()
                .map(CursoMateriaResponse::from).collect(Collectors.toList());
    }

    @Transactional
    public void desasignar(UUID idCurso, UUID idMateria) {
        UUID idInstitucion = TenantContext.get();
        CursoMateria cm = repository.findByIdInstitucionAndIdCursoAndIdMateria(idInstitucion, idCurso, idMateria)
                .orElseThrow(() -> new EntityNotFoundException("Asignación no encontrada"));
        cm.setEstado("INACTIVO");
        repository.save(cm);
    }
}
