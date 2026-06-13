package com.uagrm.si2g2.inscripcion.application;

import com.uagrm.si2g2.auditoria.application.AuditoriaService;
import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.curso.domain.Paralelo;
import com.uagrm.si2g2.curso.domain.ParaleloRepository;
import com.uagrm.si2g2.institucion.application.ConfiguracionService;
import com.uagrm.si2g2.inscripcion.domain.Inscripcion;
import com.uagrm.si2g2.inscripcion.domain.InscripcionRepository;
import com.uagrm.si2g2.inscripcion.dto.InscripcionRequest;
import com.uagrm.si2g2.inscripcion.dto.InscripcionResponse;
import com.uagrm.si2g2.pagos.application.CuotaService;
import com.uagrm.si2g2.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InscripcionService {

    private final InscripcionRepository repository;
    private final ParaleloRepository paraleloRepository;
    private final AuditoriaService auditoriaService;
    private final ConfiguracionService configuracionService;
    private final CuotaService cuotaService;

    @Transactional
    public InscripcionResponse inscribir(InscripcionRequest request) {
        UUID idInstitucion = TenantContext.get();
        if (!configuracionService.getBoolean(idInstitucion, "MATRICULA_HABILITADA")) {
            throw new IllegalStateException("La matrícula está deshabilitada para esta institución");
        }
        if (repository.existsByIdInstitucionAndIdEstudianteAndIdGestionAndEstado(
                idInstitucion, request.getIdEstudiante(), request.getIdGestion(), "ACTIVA")) {
            throw new IllegalStateException(
                    "El estudiante ya tiene una inscripción activa en esta gestión");
        }
        Paralelo paralelo = paraleloRepository.findByIdAndIdInstitucion(request.getIdParalelo(), idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Paralelo no encontrado: " + request.getIdParalelo()));
        long activeEnrollments = repository.countByIdInstitucionAndIdParaleloAndEstado(idInstitucion, paralelo.getId(), "ACTIVA");
        Integer capacity = paralelo.getCapacidad();
        if (capacity != null && activeEnrollments >= capacity) {
            throw new IllegalStateException("El paralelo alcanzó su capacidad máxima configurada");
        }

        LocalDate fecha = request.getFechaInscripcion() != null
                ? request.getFechaInscripcion()
                : LocalDate.now();
        Inscripcion i = Inscripcion.builder()
                .idInstitucion(idInstitucion)
                .idEstudiante(request.getIdEstudiante())
                .idGestion(request.getIdGestion())
                .idParalelo(request.getIdParalelo())
                .idPlanPago(request.getIdPlanPago())
                .fechaInscripcion(fecha)
                .build();
        InscripcionResponse resp = InscripcionResponse.from(repository.save(i), paralelo.getIdCurso());
        auditoriaService.registrar(idInstitucion, SecurityUtils.currentUserId(),
                "INSCRIPCION", "INSCRIBIR", "inscripcion", resp.getId().toString(),
                true, "Estudiante inscrito en gestión: " + request.getIdGestion());

        // Generar cuotas automáticamente si se asignó un plan de pago
        if (request.getIdPlanPago() != null) {
            try {
                int cuotas = cuotaService.generarCuotas(idInstitucion, request.getIdPlanPago(),
                        request.getIdGestion(), request.getIdEstudiante());
                log.info("Generadas {} cuotas para inscripción {}", cuotas, resp.getId());
            } catch (Exception e) {
                log.warn("No se pudieron generar cuotas para inscripción {}: {}", resp.getId(), e.getMessage());
            }
        }

        return resp;
    }

    @Transactional(readOnly = true)
    public List<InscripcionResponse> listar(UUID idEstudiante, UUID idGestion, UUID idParalelo) {
        UUID idInstitucion = TenantContext.get();
        List<Inscripcion> inscripciones;
        if (idEstudiante != null) {
            inscripciones = repository.findAllByIdInstitucionAndIdEstudiante(idInstitucion, idEstudiante);
        } else if (idParalelo != null) {
            inscripciones = repository.findAllByIdInstitucionAndIdParalelo(idInstitucion, idParalelo);
        } else if (idGestion != null) {
            inscripciones = repository.findAllByIdInstitucionAndIdGestion(idInstitucion, idGestion);
        } else {
            inscripciones = repository.findAllByIdInstitucion(idInstitucion);
        }
        return mapWithCurso(inscripciones, idInstitucion);
    }

    @Transactional(readOnly = true)
    public InscripcionResponse obtener(UUID id) {
        UUID idInstitucion = TenantContext.get();
        Inscripcion i = buscar(id, idInstitucion);
        UUID idCurso = paraleloRepository.findByIdAndIdInstitucion(i.getIdParalelo(), idInstitucion)
                .map(Paralelo::getIdCurso)
                .orElse(null);
        return InscripcionResponse.from(i, idCurso);
    }

    @Transactional
    public void anular(UUID id) {
        Inscripcion i = buscar(id, TenantContext.get());
        i.setEstado("ANULADA");
        repository.save(i);
        auditoriaService.registrar(TenantContext.get(), SecurityUtils.currentUserId(),
                "INSCRIPCION", "ANULAR", "inscripcion", id.toString(),
                true, "Inscripción anulada");
    }

    private Inscripcion buscar(UUID id, UUID idInstitucion) {
        return repository.findByIdAndIdInstitucion(id, idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Inscripción no encontrada: " + id));
    }

    /** Carga los paralelos necesarios en un solo batch y mapea las respuestas. */
    private List<InscripcionResponse> mapWithCurso(List<Inscripcion> inscripciones, UUID idInstitucion) {
        List<UUID> idsParalelo = inscripciones.stream()
                .map(Inscripcion::getIdParalelo)
                .distinct()
                .collect(Collectors.toList());
        Map<UUID, UUID> paraleloACurso = paraleloRepository.findAllById(idsParalelo).stream()
                .collect(Collectors.toMap(Paralelo::getId, Paralelo::getIdCurso));
        return inscripciones.stream()
                .map(i -> InscripcionResponse.from(i, paraleloACurso.get(i.getIdParalelo())))
                .collect(Collectors.toList());
    }
}
