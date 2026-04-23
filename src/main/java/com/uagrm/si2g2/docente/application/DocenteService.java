package com.uagrm.si2g2.docente.application;

import com.uagrm.si2g2.docente.domain.Docente;
import com.uagrm.si2g2.docente.domain.DocenteRepository;
import com.uagrm.si2g2.docente.dto.DocenteRequest;
import com.uagrm.si2g2.docente.dto.DocenteResponse;
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
public class DocenteService {

    private final DocenteRepository repository;

    @Transactional
    public DocenteResponse crear(DocenteRequest request) {
        UUID idInstitucion = TenantContext.get();
        if (repository.existsByIdInstitucionAndCodigo(idInstitucion, request.getCodigo())) {
            throw new IllegalStateException("Ya existe un docente con el código: " + request.getCodigo());
        }
        Docente d = Docente.builder()
                .idInstitucion(idInstitucion)
                .idUsuario(request.getIdUsuario())
                .codigo(request.getCodigo())
                .documentoIdentidad(request.getDocumentoIdentidad())
                .nombres(request.getNombres())
                .apellidos(request.getApellidos())
                .telefono(request.getTelefono())
                .correo(request.getCorreo())
                .especialidad(request.getEspecialidad())
                .build();
        return DocenteResponse.from(repository.save(d));
    }

    @Transactional(readOnly = true)
    public List<DocenteResponse> listar() {
        return repository.findAllByIdInstitucion(TenantContext.get()).stream()
                .map(DocenteResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DocenteResponse obtener(UUID id) {
        return DocenteResponse.from(buscar(id));
    }

    @Transactional
    public DocenteResponse actualizar(UUID id, DocenteRequest request) {
        UUID idInstitucion = TenantContext.get();
        Docente d = buscar(id);
        if (!d.getCodigo().equals(request.getCodigo())
                && repository.existsByIdInstitucionAndCodigo(idInstitucion, request.getCodigo())) {
            throw new IllegalStateException("Ya existe un docente con el código: " + request.getCodigo());
        }
        d.setIdUsuario(request.getIdUsuario());
        d.setCodigo(request.getCodigo());
        d.setDocumentoIdentidad(request.getDocumentoIdentidad());
        d.setNombres(request.getNombres());
        d.setApellidos(request.getApellidos());
        d.setTelefono(request.getTelefono());
        d.setCorreo(request.getCorreo());
        d.setEspecialidad(request.getEspecialidad());
        return DocenteResponse.from(repository.save(d));
    }

    @Transactional
    public void eliminar(UUID id) {
        Docente d = buscar(id);
        d.setEstado("INACTIVO");
        repository.save(d);
    }

    private Docente buscar(UUID id) {
        return repository.findByIdAndIdInstitucion(id, TenantContext.get())
                .orElseThrow(() -> new EntityNotFoundException("Docente no encontrado: " + id));
    }
}
