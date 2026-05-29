package com.uagrm.si2g2.estudiante.application;

import com.uagrm.si2g2.academico.domain.GestionAcademica;
import com.uagrm.si2g2.academico.domain.GestionAcademicaRepository;
import com.uagrm.si2g2.asignacion.domain.AsignacionDocente;
import com.uagrm.si2g2.asignacion.domain.AsignacionDocenteRepository;
import com.uagrm.si2g2.asistencia.domain.AsistenciaDetalle;
import com.uagrm.si2g2.asistencia.domain.AsistenciaDetalleRepository;
import com.uagrm.si2g2.asistencia.domain.AsistenciaRegistro;
import com.uagrm.si2g2.asistencia.domain.AsistenciaRegistroRepository;
import com.uagrm.si2g2.calificacion.domain.Calificacion;
import com.uagrm.si2g2.calificacion.domain.CalificacionRepository;
import com.uagrm.si2g2.calificacion.domain.Evaluacion;
import com.uagrm.si2g2.calificacion.domain.EvaluacionRepository;
import com.uagrm.si2g2.curso.domain.Paralelo;
import com.uagrm.si2g2.curso.domain.ParaleloRepository;
import com.uagrm.si2g2.estudiante.domain.Estudiante;
import com.uagrm.si2g2.estudiante.domain.EstudianteRepository;
import com.uagrm.si2g2.estudiante.dto.HistorialAcademicoResponse;
import com.uagrm.si2g2.estudiante.dto.HistorialEvaluacionResponse;
import com.uagrm.si2g2.estudiante.dto.HistorialGestionResponse;
import com.uagrm.si2g2.estudiante.dto.HistorialMateriaResponse;
import com.uagrm.si2g2.inscripcion.domain.Inscripcion;
import com.uagrm.si2g2.inscripcion.domain.InscripcionRepository;
import com.uagrm.si2g2.materia.domain.Materia;
import com.uagrm.si2g2.materia.domain.MateriaRepository;
import com.uagrm.si2g2.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HistorialService {

    private final EstudianteRepository estudianteRepository;
    private final InscripcionRepository inscripcionRepository;
    private final AsignacionDocenteRepository asignacionRepository;
    private final GestionAcademicaRepository gestionRepository;
    private final MateriaRepository materiaRepository;
    private final ParaleloRepository paraleloRepository;
    private final EvaluacionRepository evaluacionRepository;
    private final CalificacionRepository calificacionRepository;
    private final AsistenciaRegistroRepository asistenciaRegistroRepository;
    private final AsistenciaDetalleRepository asistenciaDetalleRepository;

    @Transactional(readOnly = true)
    public HistorialAcademicoResponse obtener(UUID idEstudiante, UUID idGestion) {
        UUID idInstitucion = TenantContext.get();

        Estudiante estudiante = estudianteRepository.findByIdAndIdInstitucion(idEstudiante, idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Estudiante no encontrado"));

        List<Inscripcion> inscripciones = idGestion != null
                ? inscripcionRepository.findAllByIdInstitucionAndIdEstudiante(idInstitucion, idEstudiante)
                        .stream()
                        .filter(i -> idGestion.equals(i.getIdGestion()))
                        .toList()
                : inscripcionRepository.findAllByIdInstitucionAndIdEstudiante(idInstitucion, idEstudiante);

        List<HistorialGestionResponse> gestiones = inscripciones.stream()
                .map(inscripcion -> buildGestionResponse(inscripcion, idInstitucion))
                .toList();

        return new HistorialAcademicoResponse(
                estudiante.getId(),
                estudiante.getCodigoEstudiante(),
                estudiante.getNombres(),
                estudiante.getApellidos(),
                gestiones
        );
    }

    private HistorialGestionResponse buildGestionResponse(Inscripcion inscripcion, UUID idInstitucion) {
        UUID idParalelo = inscripcion.getIdParalelo();
        UUID idGestionAcademica = inscripcion.getIdGestion();

        Paralelo paralelo = paraleloRepository.findByIdAndIdInstitucion(idParalelo, idInstitucion)
                .orElse(null);
        String nombreParalelo = paralelo != null ? paralelo.getNombre() : "—";

        GestionAcademica gestion = gestionRepository.findByIdAndIdInstitucion(idGestionAcademica, idInstitucion)
                .orElse(null);
        String nombreGestion = gestion != null ? gestion.getNombre() : "—";

        List<AsignacionDocente> asignaciones = asignacionRepository
                .findAllByIdInstitucionAndIdParalelo(idInstitucion, idParalelo)
                .stream()
                .filter(a -> idGestionAcademica.equals(a.getIdGestion()))
                .toList();

        List<HistorialMateriaResponse> materias = asignaciones.stream()
                .map(asignacion -> buildMateriaResponse(asignacion, inscripcion.getId(), idInstitucion))
                .toList();

        return new HistorialGestionResponse(
                idGestionAcademica,
                nombreGestion,
                idParalelo,
                nombreParalelo,
                inscripcion.getId(),
                inscripcion.getEstado(),
                inscripcion.getFechaInscripcion(),
                materias
        );
    }

    private HistorialMateriaResponse buildMateriaResponse(AsignacionDocente asignacion,
                                                           UUID idInscripcion,
                                                           UUID idInstitucion) {
        Materia materia = materiaRepository.findByIdAndIdInstitucion(asignacion.getIdMateria(), idInstitucion)
                .orElse(null);
        String codigoMateria = materia != null ? materia.getCodigo() : "—";
        String nombreMateria = materia != null ? materia.getNombre() : "—";

        List<Evaluacion> evaluaciones = evaluacionRepository
                .findAllByIdInstitucionAndIdMateria(idInstitucion, asignacion.getIdMateria());

        List<UUID> idsEvaluacion = evaluaciones.stream().map(Evaluacion::getId).toList();
        List<Calificacion> calificaciones = calificacionRepository.findAllByIdEvaluacionIn(idsEvaluacion)
                .stream()
                .filter(c -> idInscripcion.equals(c.getIdInscripcion()))
                .toList();

        Map<UUID, Calificacion> calificacionPorEvaluacion = calificaciones.stream()
                .collect(Collectors.toMap(Calificacion::getIdEvaluacion, Function.identity()));

        List<HistorialEvaluacionResponse> evalResponses = evaluaciones.stream()
                .map(ev -> {
                    Calificacion cal = calificacionPorEvaluacion.get(ev.getId());
                    return new HistorialEvaluacionResponse(
                            ev.getId(),
                            ev.getNombre(),
                            ev.getTipo(),
                            ev.getPeriodo(),
                            ev.getPonderacion(),
                            cal != null ? cal.getNotaNumerica() : null,
                            cal != null ? cal.getNotaLiteral() : null
                    );
                })
                .toList();

        BigDecimal promedio = calcularPromedio(evaluaciones, calificacionPorEvaluacion);

        // Asistencia
        List<AsistenciaRegistro> registros = asistenciaRegistroRepository
                .findAllByIdInstitucionAndIdAsignacionDocente(idInstitucion, asignacion.getId());

        int totalSesiones = registros.size();
        int sesionesPresente = 0;

        if (totalSesiones > 0) {
            List<UUID> idsRegistro = registros.stream().map(AsistenciaRegistro::getId).toList();
            List<AsistenciaDetalle> detalles = asistenciaDetalleRepository
                    .findAllByIdAsistenciaRegistroIn(idsRegistro)
                    .stream()
                    .filter(d -> idInscripcion.equals(d.getIdInscripcion()))
                    .toList();

            sesionesPresente = (int) detalles.stream()
                    .filter(d -> "PRESENTE".equalsIgnoreCase(d.getEstadoAsistencia())
                            || "TARDANZA".equalsIgnoreCase(d.getEstadoAsistencia()))
                    .count();
        }

        BigDecimal porcentaje = totalSesiones > 0
                ? BigDecimal.valueOf(sesionesPresente * 100.0 / totalSesiones).setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new HistorialMateriaResponse(
                asignacion.getIdMateria(),
                codigoMateria,
                nombreMateria,
                asignacion.getId(),
                promedio,
                evalResponses,
                totalSesiones,
                sesionesPresente,
                porcentaje
        );
    }

    private BigDecimal calcularPromedio(List<Evaluacion> evaluaciones,
                                        Map<UUID, Calificacion> calificacionPorEvaluacion) {
        BigDecimal ponderacionTotal = BigDecimal.ZERO;
        BigDecimal notaPonderada = BigDecimal.ZERO;

        for (Evaluacion ev : evaluaciones) {
            if ("ANULADA".equalsIgnoreCase(ev.getEstado())) continue;
            Calificacion cal = calificacionPorEvaluacion.get(ev.getId());
            if (cal != null && cal.getNotaNumerica() != null) {
                notaPonderada = notaPonderada.add(
                        cal.getNotaNumerica().multiply(ev.getPonderacion())
                );
                ponderacionTotal = ponderacionTotal.add(ev.getPonderacion());
            }
        }

        if (ponderacionTotal.compareTo(BigDecimal.ZERO) == 0) return null;
        return notaPonderada.divide(ponderacionTotal, 2, RoundingMode.HALF_UP);
    }
}
