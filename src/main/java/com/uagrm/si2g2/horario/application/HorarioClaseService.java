package com.uagrm.si2g2.horario.application;

import com.uagrm.si2g2.asignacion.domain.AsignacionDocente;
import com.uagrm.si2g2.asignacion.domain.AsignacionDocenteRepository;
import com.uagrm.si2g2.aula.domain.AulaRepository;
import com.uagrm.si2g2.horario.domain.HorarioClase;
import com.uagrm.si2g2.horario.domain.HorarioClaseRepository;
import com.uagrm.si2g2.horario.dto.HorarioClaseRequest;
import com.uagrm.si2g2.horario.dto.HorarioClaseResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HorarioClaseService {

    private static final String ESTADO_ACTIVO = "ACTIVO";
    private static final String ESTADO_INACTIVO = "INACTIVO";
    private static final Set<String> DIAS_PERMITIDOS = Set.of(
            "LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO");

    private final HorarioClaseRepository repository;
    private final AsignacionDocenteRepository asignacionDocenteRepository;
    private final AulaRepository aulaRepository;

    @Transactional(readOnly = true)
    public List<HorarioClaseResponse> listarActivos(UUID idInstitucion) {
        return repository.findByIdInstitucionAndEstado(idInstitucion, ESTADO_ACTIVO)
                .stream()
                .map(HorarioClaseResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public HorarioClaseResponse obtenerPorId(UUID id) {
        return HorarioClaseResponse.from(buscar(id));
    }

    @Transactional
    public HorarioClaseResponse crear(HorarioClaseRequest request) {
        String diaSemana = normalizarDia(request.getDiaSemana());
        AsignacionDocente asignacion = obtenerAsignacion(request);
        validarAula(request);
        validarRequest(request, diaSemana, asignacion, null);

        HorarioClase horario = HorarioClase.builder()
                .idInstitucion(request.getIdInstitucion())
                .idAsignacionDocente(request.getIdAsignacionDocente())
                .idAula(request.getIdAula())
                .diaSemana(diaSemana)
                .horaInicio(request.getHoraInicio())
                .horaFin(request.getHoraFin())
                .build();

        return HorarioClaseResponse.from(repository.save(horario));
    }

    @Transactional
    public HorarioClaseResponse actualizar(UUID id, HorarioClaseRequest request) {
        HorarioClase horario = buscar(id);
        String diaSemana = normalizarDia(request.getDiaSemana());
        AsignacionDocente asignacion = obtenerAsignacion(request);
        validarAula(request);
        validarRequest(request, diaSemana, asignacion, id);

        horario.setIdInstitucion(request.getIdInstitucion());
        horario.setIdAsignacionDocente(request.getIdAsignacionDocente());
        horario.setIdAula(request.getIdAula());
        horario.setDiaSemana(diaSemana);
        horario.setHoraInicio(request.getHoraInicio());
        horario.setHoraFin(request.getHoraFin());

        return HorarioClaseResponse.from(repository.save(horario));
    }

    @Transactional
    public void eliminar(UUID id) {
        HorarioClase horario = buscar(id);
        horario.setEstado(ESTADO_INACTIVO);
        repository.save(horario);
    }

    @Transactional(readOnly = true)
    public List<HorarioClaseResponse> listarPorAsignacionDocente(UUID idAsignacionDocente) {
        return repository.findByIdAsignacionDocenteAndEstado(idAsignacionDocente, ESTADO_ACTIVO)
                .stream()
                .map(HorarioClaseResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HorarioClaseResponse> listarPorAula(UUID idAula) {
        return repository.findByIdAulaAndEstado(idAula, ESTADO_ACTIVO)
                .stream()
                .map(HorarioClaseResponse::from)
                .toList();
    }

    private HorarioClase buscar(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Horario no encontrado: " + id));
    }

    private AsignacionDocente obtenerAsignacion(HorarioClaseRequest request) {
        return asignacionDocenteRepository
                .findByIdAndIdInstitucion(request.getIdAsignacionDocente(), request.getIdInstitucion())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Asignacion docente no encontrada: " + request.getIdAsignacionDocente()));
    }

    private void validarAula(HorarioClaseRequest request) {
        aulaRepository.findByIdAndIdInstitucion(request.getIdAula(), request.getIdInstitucion())
                .orElseThrow(() -> new EntityNotFoundException("Aula no encontrada: " + request.getIdAula()));
    }

    private void validarRequest(HorarioClaseRequest request, String diaSemana,
                                AsignacionDocente asignacion, UUID idExcluir) {
        if (!request.getHoraInicio().isBefore(request.getHoraFin())) {
            throw new IllegalArgumentException("La hora de inicio debe ser menor que la hora fin");
        }
        if (!DIAS_PERMITIDOS.contains(diaSemana)) {
            throw new IllegalArgumentException("Dia de semana no permitido: " + request.getDiaSemana());
        }
        validarConflictos(request, diaSemana, asignacion, idExcluir);
    }

    private void validarConflictos(HorarioClaseRequest request, String diaSemana,
                                   AsignacionDocente asignacion, UUID idExcluir) {
        if (!repository.buscarConflictoAula(request.getIdInstitucion(), request.getIdAula(), diaSemana,
                request.getHoraInicio(), request.getHoraFin(), idExcluir).isEmpty()) {
            throw new IllegalStateException("El aula ya tiene una clase en el horario indicado");
        }
        if (!repository.buscarConflictoDocente(request.getIdInstitucion(), asignacion.getIdDocente(), diaSemana,
                request.getHoraInicio(), request.getHoraFin(), idExcluir).isEmpty()) {
            throw new IllegalStateException("El docente ya tiene una clase en el horario indicado");
        }
        if (!repository.buscarConflictoParalelo(request.getIdInstitucion(), asignacion.getIdParalelo(), diaSemana,
                request.getHoraInicio(), request.getHoraFin(), idExcluir).isEmpty()) {
            throw new IllegalStateException("El paralelo ya tiene una clase en el horario indicado");
        }
    }

    private String normalizarDia(String diaSemana) {
        return diaSemana == null ? null : diaSemana.trim().toUpperCase();
    }
}
