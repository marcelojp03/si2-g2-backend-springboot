package com.uagrm.si2g2.tutor.application;

import com.uagrm.si2g2.tutor.domain.Tutor;
import com.uagrm.si2g2.tutor.domain.TutorRepository;
import com.uagrm.si2g2.tutor.dto.TutorRequest;
import com.uagrm.si2g2.tutor.dto.TutorResponse;
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
public class TutorService {

    private final TutorRepository repository;

    @Transactional
    public TutorResponse crear(TutorRequest request) {
        UUID idInstitucion = TenantContext.get();
        Tutor t = Tutor.builder()
                .idInstitucion(idInstitucion)
                .documentoIdentidad(request.getDocumentoIdentidad())
                .nombres(request.getNombres())
                .apellidos(request.getApellidos())
                .telefono(request.getTelefono())
                .correo(request.getCorreo())
                .build();
        return TutorResponse.from(repository.save(t));
    }

    @Transactional(readOnly = true)
    public List<TutorResponse> listar() {
        return repository.findAllByIdInstitucion(TenantContext.get()).stream()
                .map(TutorResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TutorResponse obtener(UUID id) {
        return TutorResponse.from(buscar(id));
    }

    @Transactional
    public TutorResponse actualizar(UUID id, TutorRequest request) {
        Tutor t = buscar(id);
        t.setDocumentoIdentidad(request.getDocumentoIdentidad());
        t.setNombres(request.getNombres());
        t.setApellidos(request.getApellidos());
        t.setTelefono(request.getTelefono());
        t.setCorreo(request.getCorreo());
        return TutorResponse.from(repository.save(t));
    }

    @Transactional
    public void eliminar(UUID id) {
        Tutor t = buscar(id);
        t.setEstado("INACTIVO");
        repository.save(t);
    }

    private Tutor buscar(UUID id) {
        return repository.findByIdAndIdInstitucion(id, TenantContext.get())
                .orElseThrow(() -> new EntityNotFoundException("Tutor no encontrado: " + id));
    }
}
