package com.uagrm.si2g2.institucion.application;

import com.uagrm.si2g2.institucion.domain.ConfiguracionInstitucion;
import com.uagrm.si2g2.institucion.domain.ConfiguracionInstitucionRepository;
import com.uagrm.si2g2.institucion.domain.Institucion;
import com.uagrm.si2g2.institucion.domain.InstitucionRepository;
import com.uagrm.si2g2.institucion.dto.ConfiguracionInstitucionRequest;
import com.uagrm.si2g2.institucion.dto.ConfiguracionInstitucionResponse;
import com.uagrm.si2g2.institucion.dto.InstitucionRequest;
import com.uagrm.si2g2.institucion.dto.InstitucionResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InstitucionService {

    private final InstitucionRepository institucionRepository;
    private final ConfiguracionInstitucionRepository configuracionRepository;

    @Transactional
    public InstitucionResponse crear(InstitucionRequest request) {
        if (institucionRepository.existsByCodigo(request.getCodigo())) {
            throw new IllegalStateException("Ya existe una institución con el código: " + request.getCodigo());
        }
        Institucion inst = Institucion.builder()
                .codigo(request.getCodigo())
                .nombre(request.getNombre())
                .tipoInstitucion(request.getTipoInstitucion())
                .telefono(request.getTelefono())
                .correo(request.getCorreo())
                .direccion(request.getDireccion())
                .logoUrl(request.getLogoUrl())
                .build();
        return InstitucionResponse.from(institucionRepository.save(inst));
    }

    @Transactional(readOnly = true)
    public List<InstitucionResponse> listar() {
        return institucionRepository.findAll().stream()
                .map(InstitucionResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InstitucionResponse obtener(UUID id) {
        return institucionRepository.findById(id)
                .map(InstitucionResponse::from)
                .orElseThrow(() -> new EntityNotFoundException("Institución no encontrada: " + id));
    }

    @Transactional
    public InstitucionResponse actualizar(UUID id, InstitucionRequest request) {
        Institucion inst = institucionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Institución no encontrada: " + id));

        if (!inst.getCodigo().equals(request.getCodigo())
                && institucionRepository.existsByCodigo(request.getCodigo())) {
            throw new IllegalStateException("Ya existe una institución con el código: " + request.getCodigo());
        }

        inst.setCodigo(request.getCodigo());
        inst.setNombre(request.getNombre());
        inst.setTipoInstitucion(request.getTipoInstitucion());
        inst.setTelefono(request.getTelefono());
        inst.setCorreo(request.getCorreo());
        inst.setDireccion(request.getDireccion());
        inst.setLogoUrl(request.getLogoUrl());
        return InstitucionResponse.from(institucionRepository.save(inst));
    }

    // --- Configuración ---

    @Transactional(readOnly = true)
    public List<ConfiguracionInstitucionResponse> listarConfiguraciones(UUID idInstitucion) {
        if (!institucionRepository.existsById(idInstitucion)) {
            throw new EntityNotFoundException("Institución no encontrada: " + idInstitucion);
        }
        return configuracionRepository.findAllByIdInstitucion(idInstitucion).stream()
                .map(ConfiguracionInstitucionResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public ConfiguracionInstitucionResponse guardarConfiguracion(UUID idInstitucion,
                                                                  ConfiguracionInstitucionRequest request) {
        if (!institucionRepository.existsById(idInstitucion)) {
            throw new EntityNotFoundException("Institución no encontrada: " + idInstitucion);
        }

        ConfiguracionInstitucion config = configuracionRepository
                .findByIdInstitucionAndClave(idInstitucion, request.getClave())
                .orElse(ConfiguracionInstitucion.builder()
                        .idInstitucion(idInstitucion)
                        .clave(request.getClave())
                        .build());

        config.setValor(request.getValor());
        config.setTipoValor(request.getTipoValor() != null ? request.getTipoValor() : "TEXTO");
        config.setDescripcion(request.getDescripcion());

        return ConfiguracionInstitucionResponse.from(configuracionRepository.save(config));
    }
}
