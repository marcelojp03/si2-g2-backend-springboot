package com.uagrm.si2g2.curso.application;

import com.uagrm.si2g2.curso.domain.Curso;
import com.uagrm.si2g2.curso.domain.CursoRepository;
import com.uagrm.si2g2.curso.dto.CursoRequest;
import com.uagrm.si2g2.curso.dto.CursoResponse;
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
public class CursoService {

    private final CursoRepository repository;

    @Transactional
    public CursoResponse crear(CursoRequest request) {
        UUID idInstitucion = TenantContext.get();
        if (repository.existsByIdInstitucionAndNombre(idInstitucion, request.getNombre())) {
            throw new IllegalStateException("Ya existe un curso con el nombre: " + request.getNombre());
        }
        Curso curso = Curso.builder()
                .idInstitucion(idInstitucion)
                .codigo(request.getCodigo())
                .nombre(request.getNombre())
                .nivel(request.getNivel())
                .build();
        return CursoResponse.from(repository.save(curso));
    }

    @Transactional(readOnly = true)
    public List<CursoResponse> listar() {
        return repository.findAllByIdInstitucion(TenantContext.get()).stream()
                .map(CursoResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CursoResponse obtener(UUID id) {
        return CursoResponse.from(buscar(id));
    }

    @Transactional
    public CursoResponse actualizar(UUID id, CursoRequest request) {
        UUID idInstitucion = TenantContext.get();
        Curso curso = buscar(id);
        if (!curso.getNombre().equals(request.getNombre())
                && repository.existsByIdInstitucionAndNombre(idInstitucion, request.getNombre())) {
            throw new IllegalStateException("Ya existe un curso con el nombre: " + request.getNombre());
        }
        curso.setCodigo(request.getCodigo());
        curso.setNombre(request.getNombre());
        curso.setNivel(request.getNivel());
        return CursoResponse.from(repository.save(curso));
    }

    @Transactional
    public void eliminar(UUID id) {
        Curso curso = buscar(id);
        curso.setEstado("INACTIVO");
        repository.save(curso);
    }

    private Curso buscar(UUID id) {
        return repository.findByIdAndIdInstitucion(id, TenantContext.get())
                .orElseThrow(() -> new EntityNotFoundException("Curso no encontrado: " + id));
    }
}
