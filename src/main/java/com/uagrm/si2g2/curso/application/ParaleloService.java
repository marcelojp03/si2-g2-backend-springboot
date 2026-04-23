package com.uagrm.si2g2.curso.application;

import com.uagrm.si2g2.curso.domain.Paralelo;
import com.uagrm.si2g2.curso.domain.ParaleloRepository;
import com.uagrm.si2g2.curso.dto.ParaleloRequest;
import com.uagrm.si2g2.curso.dto.ParaleloResponse;
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
public class ParaleloService {

    private final ParaleloRepository repository;

    @Transactional
    public ParaleloResponse crear(ParaleloRequest request) {
        UUID idInstitucion = TenantContext.get();
        if (repository.existsByIdInstitucionAndIdCursoAndNombreAndIdGestion(
                idInstitucion, request.getIdCurso(), request.getNombre(), request.getIdGestion())) {
            throw new IllegalStateException(
                    "Ya existe el paralelo '" + request.getNombre() + "' para ese curso y gestión");
        }
        Paralelo p = Paralelo.builder()
                .idInstitucion(idInstitucion)
                .idCurso(request.getIdCurso())
                .idGestion(request.getIdGestion())
                .nombre(request.getNombre())
                .capacidad(request.getCapacidad())
                .build();
        return ParaleloResponse.from(repository.save(p));
    }

    @Transactional(readOnly = true)
    public List<ParaleloResponse> listar(UUID idCurso) {
        UUID idInstitucion = TenantContext.get();
        if (idCurso != null) {
            return repository.findAllByIdInstitucionAndIdCurso(idInstitucion, idCurso).stream()
                    .map(ParaleloResponse::from).collect(Collectors.toList());
        }
        return repository.findAllByIdInstitucion(idInstitucion).stream()
                .map(ParaleloResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ParaleloResponse obtener(UUID id) {
        return ParaleloResponse.from(buscar(id));
    }

    @Transactional
    public ParaleloResponse actualizar(UUID id, ParaleloRequest request) {
        UUID idInstitucion = TenantContext.get();
        Paralelo p = buscar(id);
        if (!p.getNombre().equals(request.getNombre())
                && repository.existsByIdInstitucionAndIdCursoAndNombreAndIdGestion(
                        idInstitucion, request.getIdCurso(), request.getNombre(), request.getIdGestion())) {
            throw new IllegalStateException(
                    "Ya existe el paralelo '" + request.getNombre() + "' para ese curso y gestión");
        }
        p.setIdCurso(request.getIdCurso());
        p.setIdGestion(request.getIdGestion());
        p.setNombre(request.getNombre());
        p.setCapacidad(request.getCapacidad());
        return ParaleloResponse.from(repository.save(p));
    }

    @Transactional
    public void eliminar(UUID id) {
        Paralelo p = buscar(id);
        p.setEstado("INACTIVO");
        repository.save(p);
    }

    private Paralelo buscar(UUID id) {
        return repository.findByIdAndIdInstitucion(id, TenantContext.get())
                .orElseThrow(() -> new EntityNotFoundException("Paralelo no encontrado: " + id));
    }
}
