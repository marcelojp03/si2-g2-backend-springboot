package com.uagrm.si2g2.curso.application;

import com.uagrm.si2g2.auditoria.application.AuditoriaService;
import com.uagrm.si2g2.common.SecurityUtils;
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
    private final AuditoriaService auditoriaService;

    @Transactional
    public ParaleloResponse crear(ParaleloRequest request) {
        UUID idInstitucion = TenantContext.get();
        if (repository.existsByIdInstitucionAndIdCursoAndNombreAndIdGestionAcademica(
                idInstitucion, request.getIdCurso(), request.getNombre(), request.getIdGestionAcademica())) {
            throw new IllegalStateException(
                    "Ya existe el paralelo '" + request.getNombre() + "' para ese curso y gestión");
        }
        Paralelo p = Paralelo.builder()
                .idInstitucion(idInstitucion)
                .idCurso(request.getIdCurso())
                .idGestionAcademica(request.getIdGestionAcademica())
                .nombre(request.getNombre())
                .capacidad(request.getCapacidad())
                .build();
        ParaleloResponse resp = ParaleloResponse.from(repository.save(p));
        auditoriaService.registrar(idInstitucion, SecurityUtils.currentUserId(),
                "CURSO", "CREAR_PARALELO", "paralelo", resp.getId().toString(),
                true, "Paralelo creado: " + resp.getNombre());
        return resp;
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
                && repository.existsByIdInstitucionAndIdCursoAndNombreAndIdGestionAcademica(
                        idInstitucion, request.getIdCurso(), request.getNombre(), request.getIdGestionAcademica())) {
            throw new IllegalStateException(
                    "Ya existe el paralelo '" + request.getNombre() + "' para ese curso y gestión");
        }
        p.setIdCurso(request.getIdCurso());
        p.setIdGestionAcademica(request.getIdGestionAcademica());
        p.setNombre(request.getNombre());
        p.setCapacidad(request.getCapacidad());
        ParaleloResponse resp = ParaleloResponse.from(repository.save(p));
        auditoriaService.registrar(TenantContext.get(), SecurityUtils.currentUserId(),
                "CURSO", "ACTUALIZAR_PARALELO", "paralelo", id.toString(),
                true, "Paralelo actualizado: " + resp.getNombre());
        return resp;
    }

    @Transactional
    public void eliminar(UUID id) {
        Paralelo p = buscar(id);
        p.setEstado("INACTIVO");
        repository.save(p);
        auditoriaService.registrar(TenantContext.get(), SecurityUtils.currentUserId(),
                "CURSO", "ELIMINAR_PARALELO", "paralelo", id.toString(),
                true, "Paralelo desactivado: " + p.getNombre());
    }

    private Paralelo buscar(UUID id) {
        return repository.findByIdAndIdInstitucion(id, TenantContext.get())
                .orElseThrow(() -> new EntityNotFoundException("Paralelo no encontrado: " + id));
    }
}
