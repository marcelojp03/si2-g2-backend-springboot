package com.uagrm.si2g2.academico.application;

import com.uagrm.si2g2.academico.domain.GestionAcademica;
import com.uagrm.si2g2.academico.domain.GestionAcademicaRepository;
import com.uagrm.si2g2.academico.dto.GestionAcademicaRequest;
import com.uagrm.si2g2.academico.dto.GestionAcademicaResponse;
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
public class GestionAcademicaService {

    private final GestionAcademicaRepository repository;

    @Transactional
    public GestionAcademicaResponse crear(GestionAcademicaRequest request) {
        UUID idInstitucion = TenantContext.get();
        if (repository.existsByIdInstitucionAndNombre(idInstitucion, request.getNombre())) {
            throw new IllegalStateException("Ya existe una gestión con el nombre: " + request.getNombre());
        }
        if (request.isActiva()) {
            repository.desactivarTodasPorInstitucion(idInstitucion);
        }
        GestionAcademica g = GestionAcademica.builder()
                .idInstitucion(idInstitucion)
                .nombre(request.getNombre())
                .fechaInicio(request.getFechaInicio())
                .fechaFin(request.getFechaFin())
                .activa(request.isActiva())
                .build();
        return GestionAcademicaResponse.from(repository.save(g));
    }

    @Transactional(readOnly = true)
    public List<GestionAcademicaResponse> listar() {
        return repository.findAllByIdInstitucion(TenantContext.get()).stream()
                .map(GestionAcademicaResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GestionAcademicaResponse obtener(UUID id) {
        return GestionAcademicaResponse.from(buscar(id));
    }

    @Transactional
    public GestionAcademicaResponse actualizar(UUID id, GestionAcademicaRequest request) {
        UUID idInstitucion = TenantContext.get();
        GestionAcademica g = buscar(id);

        if (!g.getNombre().equals(request.getNombre())
                && repository.existsByIdInstitucionAndNombre(idInstitucion, request.getNombre())) {
            throw new IllegalStateException("Ya existe una gestión con el nombre: " + request.getNombre());
        }
        if (request.isActiva() && !g.isActiva()) {
            repository.desactivarTodasPorInstitucion(idInstitucion);
        }
        g.setNombre(request.getNombre());
        g.setFechaInicio(request.getFechaInicio());
        g.setFechaFin(request.getFechaFin());
        g.setActiva(request.isActiva());
        return GestionAcademicaResponse.from(repository.save(g));
    }

    @Transactional
    public void eliminar(UUID id) {
        GestionAcademica g = buscar(id);
        g.setEstado("ANULADA");
        g.setActiva(false);
        repository.save(g);
    }

    private GestionAcademica buscar(UUID id) {
        UUID idInstitucion = TenantContext.get();
        return repository.findByIdAndIdInstitucion(id, idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Gestión académica no encontrada: " + id));
    }
}
