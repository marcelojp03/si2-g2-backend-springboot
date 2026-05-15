package com.uagrm.si2g2.aula.application;

import com.uagrm.si2g2.auditoria.application.AuditoriaService;
import com.uagrm.si2g2.aula.domain.Aula;
import com.uagrm.si2g2.aula.domain.AulaRepository;
import com.uagrm.si2g2.aula.dto.AulaRequest;
import com.uagrm.si2g2.aula.dto.AulaResponse;
import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AulaService {

    private final AulaRepository repository;
    private final AulaUsoService aulaUsoService;
    private final AuditoriaService auditoriaService;

    @Transactional
    public AulaResponse crear(AulaRequest request) {
        UUID idInstitucion = TenantContext.get();
        validarUnicos(idInstitucion, null, request);

        Aula aula = Aula.builder()
                .idInstitucion(idInstitucion)
                .codigo(normalizeRequired(request.getCodigo()))
                .nombre(normalizeRequired(request.getNombre()))
                .capacidad(request.getCapacidad())
                .ubicacion(normalizeOptional(request.getUbicacion()))
                .recursos(joinRecursos(request.getRecursos()))
                .build();

        AulaResponse response = AulaResponse.from(repository.save(aula));
        auditoriaService.registrar(idInstitucion, SecurityUtils.currentUserId(),
                "GESTION_ACADEMICA", "CREAR_AULA", "aula", response.getId().toString(),
                true, "Aula creada: " + response.getCodigo());
        return response;
    }

    @Transactional(readOnly = true)
    public List<AulaResponse> listar(String estado, Integer capacidadMin, Integer capacidadMax, String recurso, String q) {
        if (capacidadMin != null && capacidadMax != null && capacidadMin > capacidadMax) {
            throw new IllegalArgumentException("La capacidad mínima no puede ser mayor a la capacidad máxima");
        }
        String normalizedEstado = normalizeOptional(estado);
        String normalizedRecurso = normalizeLower(recurso);
        String normalizedQuery = normalizeLower(q);
        return repository.findAllByIdInstitucionOrderByEstadoAscNombreAsc(TenantContext.get())
                .stream()
                .filter(aula -> normalizedEstado == null || normalizedEstado.equals(aula.getEstado()))
                .filter(aula -> capacidadMin == null || aula.getCapacidad() >= capacidadMin)
                .filter(aula -> capacidadMax == null || aula.getCapacidad() <= capacidadMax)
                .filter(aula -> normalizedRecurso == null || containsIgnoreCase(aula.getRecursos(), normalizedRecurso))
                .filter(aula -> normalizedQuery == null
                        || containsIgnoreCase(aula.getCodigo(), normalizedQuery)
                        || containsIgnoreCase(aula.getNombre(), normalizedQuery)
                        || containsIgnoreCase(aula.getUbicacion(), normalizedQuery))
                .map(AulaResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AulaResponse obtener(UUID id) {
        return AulaResponse.from(buscar(id));
    }

    @Transactional
    public AulaResponse actualizar(UUID id, AulaRequest request) {
        UUID idInstitucion = TenantContext.get();
        Aula aula = buscar(id);
        validarUnicos(idInstitucion, aula, request);

        aula.setCodigo(normalizeRequired(request.getCodigo()));
        aula.setNombre(normalizeRequired(request.getNombre()));
        aula.setCapacidad(request.getCapacidad());
        aula.setUbicacion(normalizeOptional(request.getUbicacion()));
        aula.setRecursos(joinRecursos(request.getRecursos()));

        AulaResponse response = AulaResponse.from(repository.save(aula));
        auditoriaService.registrar(idInstitucion, SecurityUtils.currentUserId(),
                "GESTION_ACADEMICA", "ACTUALIZAR_AULA", "aula", id.toString(),
                true, "Aula actualizada: " + response.getCodigo());
        return response;
    }

    @Transactional
    public void eliminar(UUID id) {
        UUID idInstitucion = TenantContext.get();
        Aula aula = buscar(id);
        if (aulaUsoService.tieneHorariosActivosEnGestionActual(idInstitucion, id)) {
            throw new IllegalStateException("No se pudo desactivar: el aula tiene horarios activos");
        }
        aula.setEstado("INACTIVO");
        repository.save(aula);
        auditoriaService.registrar(idInstitucion, SecurityUtils.currentUserId(),
                "GESTION_ACADEMICA", "DESACTIVAR_AULA", "aula", id.toString(),
                true, "Aula desactivada: " + aula.getCodigo());
    }

    private Aula buscar(UUID id) {
        return repository.findByIdAndIdInstitucion(id, TenantContext.get())
                .orElseThrow(() -> new EntityNotFoundException("Aula no encontrada: " + id));
    }

    private void validarUnicos(UUID idInstitucion, Aula actual, AulaRequest request) {
        String codigo = normalizeRequired(request.getCodigo());
        String nombre = normalizeRequired(request.getNombre());
        if ((actual == null || !actual.getCodigo().equals(codigo))
                && repository.existsByIdInstitucionAndCodigo(idInstitucion, codigo)) {
            throw new IllegalStateException("Ya existe un aula con el código: " + codigo);
        }
        if ((actual == null || !actual.getNombre().equals(nombre))
                && repository.existsByIdInstitucionAndNombre(idInstitucion, nombre)) {
            throw new IllegalStateException("Ya existe un aula con el nombre: " + nombre);
        }
    }

    private String joinRecursos(List<String> recursos) {
        if (recursos == null) {
            return null;
        }
        String value = recursos.stream()
                .map(this::normalizeOptional)
                .filter(item -> item != null && !item.contains("|"))
                .distinct()
                .limit(20)
                .reduce((left, right) -> left + "|" + right)
                .orElse(null);
        if (value != null && value.length() > 500) {
            throw new IllegalArgumentException("La lista de recursos es demasiado larga");
        }
        return value;
    }

    private String normalizeRequired(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeLower(String value) {
        String normalized = normalizeOptional(value);
        return normalized == null ? null : normalized.toLowerCase();
    }

    private boolean containsIgnoreCase(String source, String query) {
        return source != null && source.toLowerCase().contains(query);
    }
}
