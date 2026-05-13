package com.uagrm.si2g2.institucion.application;

import com.uagrm.si2g2.auditoria.application.AuditoriaService;
import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.institucion.domain.ConfiguracionInstitucion;
import com.uagrm.si2g2.institucion.domain.Institucion;
import com.uagrm.si2g2.institucion.domain.InstitucionRepository;
import com.uagrm.si2g2.institucion.dto.ConfiguracionInstitucionRequest;
import com.uagrm.si2g2.institucion.dto.ConfiguracionParametroResponse;
import com.uagrm.si2g2.institucion.dto.ConfiguracionInstitucionResponse;
import com.uagrm.si2g2.institucion.dto.InstitucionRequest;
import com.uagrm.si2g2.institucion.dto.InstitucionResponse;
import com.uagrm.si2g2.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InstitucionService {

    private final InstitucionRepository institucionRepository;
    private final AuditoriaService auditoriaService;
    private final ConfiguracionService configuracionService;

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
                .build();
        InstitucionResponse resp = InstitucionResponse.from(institucionRepository.save(inst));
        auditoriaService.registrar(resp.getId(), SecurityUtils.currentUserId(),
                "INSTITUCION", "CREAR", "institucion", resp.getId().toString(),
                true, "Institución creada: " + resp.getCodigo());
        return resp;
    }

    @Transactional(readOnly = true)
    public List<InstitucionResponse> listar() {
        return institucionRepository.findAll().stream()
                .map(InstitucionResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InstitucionResponse obtener(UUID id) {
        UUID accessibleInstitutionId = resolveAccessibleInstitutionId(id);
        return institucionRepository.findById(accessibleInstitutionId)
                .map(InstitucionResponse::from)
                .orElseThrow(() -> new EntityNotFoundException("Institución no encontrada: " + accessibleInstitutionId));
    }

    @Transactional(readOnly = true)
    public InstitucionResponse obtenerActual() {
        return obtener(requireInstitutionTenant());
    }

    @Transactional
    public InstitucionResponse actualizar(UUID id, InstitucionRequest request) {
        UUID accessibleInstitutionId = resolveAccessibleInstitutionId(id);
        Institucion inst = institucionRepository.findById(accessibleInstitutionId)
                .orElseThrow(() -> new EntityNotFoundException("Institución no encontrada: " + accessibleInstitutionId));

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
        InstitucionResponse resp = InstitucionResponse.from(institucionRepository.save(inst));
        auditoriaService.registrar(accessibleInstitutionId, SecurityUtils.currentUserId(),
                "INSTITUCION", "ACTUALIZAR", "institucion", accessibleInstitutionId.toString(),
                true, "Institución actualizada: " + resp.getCodigo());
        return resp;
    }

    // --- Configuración ---

    @Transactional(readOnly = true)
    public List<ConfiguracionInstitucionResponse> listarConfiguraciones(UUID idInstitucion) {
        UUID accessibleInstitutionId = resolveAccessibleInstitutionId(idInstitucion);
        return configuracionService.listarSoportadas(accessibleInstitutionId).stream()
                .map(item -> ConfiguracionInstitucionResponse.builder()
                        .id(null)
                        .idInstitucion(accessibleInstitutionId)
                        .clave(item.getClave())
                        .valor(item.getValor())
                        .tipoValor(item.getTipoValor())
                        .descripcion(item.getDescripcion())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ConfiguracionInstitucionResponse> listarConfiguracionesActuales() {
        return listarConfiguraciones(requireInstitutionTenant());
    }

    @Transactional
    public ConfiguracionInstitucionResponse guardarConfiguracion(UUID idInstitucion,
                                                                  ConfiguracionInstitucionRequest request) {
        UUID accessibleInstitutionId = resolveAccessibleInstitutionId(idInstitucion);
        return configuracionService.guardar(accessibleInstitutionId, request);
    }

    @Transactional
    public ConfiguracionInstitucionResponse guardarConfiguracionActual(ConfiguracionInstitucionRequest request) {
        return guardarConfiguracion(requireInstitutionTenant(), request);
    }

    @Transactional
    public void eliminarConfiguracion(UUID idInstitucion, String clave) {
        UUID accessibleInstitutionId = resolveAccessibleInstitutionId(idInstitucion);
        configuracionService.resetToDefault(accessibleInstitutionId, clave);
    }

    @Transactional
    public void eliminarConfiguracionActual(String clave) {
        eliminarConfiguracion(requireInstitutionTenant(), clave);
    }

    @Transactional(readOnly = true)
    public List<ConfiguracionParametroResponse> listarCatalogoConfiguracionesActuales() {
        return configuracionService.listarSoportadas(requireInstitutionTenant());
    }

    private UUID resolveAccessibleInstitutionId(UUID requestedInstitutionId) {
        if (SecurityUtils.currentUserHasRole("SUPER_ADMIN")) {
            return requestedInstitutionId;
        }

        UUID tenantInstitutionId = requireInstitutionTenant();
        if (!tenantInstitutionId.equals(requestedInstitutionId)) {
            throw new AccessDeniedException("No tienes acceso a otra institución");
        }
        return tenantInstitutionId;
    }

    private UUID requireInstitutionTenant() {
        UUID tenantInstitutionId = TenantContext.get();
        return tenantInstitutionId != null ? tenantInstitutionId : SecurityUtils.requireCurrentInstitutionId();
    }
}
