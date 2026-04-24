package com.uagrm.si2g2.materia.application;

import com.uagrm.si2g2.auditoria.application.AuditoriaService;
import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.materia.domain.Materia;
import com.uagrm.si2g2.materia.domain.MateriaRepository;
import com.uagrm.si2g2.materia.dto.MateriaRequest;
import com.uagrm.si2g2.materia.dto.MateriaResponse;
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
public class MateriaService {

    private final MateriaRepository repository;
    private final AuditoriaService auditoriaService;

    @Transactional
    public MateriaResponse crear(MateriaRequest request) {
        UUID idInstitucion = TenantContext.get();
        if (repository.existsByIdInstitucionAndCodigo(idInstitucion, request.getCodigo())) {
            throw new IllegalStateException("Ya existe una materia con el código: " + request.getCodigo());
        }
        Materia m = Materia.builder()
                .idInstitucion(idInstitucion)
                .codigo(request.getCodigo())
                .nombre(request.getNombre())
                .area(request.getArea())
                .cargaHoraria(request.getCargaHoraria())
                .build();
        MateriaResponse resp = MateriaResponse.from(repository.save(m));
        auditoriaService.registrar(idInstitucion, SecurityUtils.currentUserId(),
                "MATERIA", "CREAR", "materia", resp.getId().toString(),
                true, "Materia creada: " + resp.getCodigo());
        return resp;
    }

    @Transactional(readOnly = true)
    public List<MateriaResponse> listar() {
        return repository.findAllByIdInstitucion(TenantContext.get()).stream()
                .map(MateriaResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MateriaResponse obtener(UUID id) {
        return MateriaResponse.from(buscar(id));
    }

    @Transactional
    public MateriaResponse actualizar(UUID id, MateriaRequest request) {
        UUID idInstitucion = TenantContext.get();
        Materia m = buscar(id);
        if (!m.getCodigo().equals(request.getCodigo())
                && repository.existsByIdInstitucionAndCodigo(idInstitucion, request.getCodigo())) {
            throw new IllegalStateException("Ya existe una materia con el código: " + request.getCodigo());
        }
        m.setCodigo(request.getCodigo());
        m.setNombre(request.getNombre());
        m.setArea(request.getArea());
        m.setCargaHoraria(request.getCargaHoraria());
        MateriaResponse resp = MateriaResponse.from(repository.save(m));
        auditoriaService.registrar(TenantContext.get(), SecurityUtils.currentUserId(),
                "MATERIA", "ACTUALIZAR", "materia", id.toString(),
                true, "Materia actualizada: " + resp.getCodigo());
        return resp;
    }

    @Transactional
    public void eliminar(UUID id) {
        Materia m = buscar(id);
        m.setEstado("INACTIVO");
        repository.save(m);
        auditoriaService.registrar(TenantContext.get(), SecurityUtils.currentUserId(),
                "MATERIA", "ELIMINAR", "materia", id.toString(),
                true, "Materia desactivada: " + m.getCodigo());
    }

    private Materia buscar(UUID id) {
        return repository.findByIdAndIdInstitucion(id, TenantContext.get())
                .orElseThrow(() -> new EntityNotFoundException("Materia no encontrada: " + id));
    }
}
