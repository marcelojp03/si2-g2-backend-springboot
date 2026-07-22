package com.uagrm.si2g2.alertas.application;

import com.uagrm.si2g2.alertas.domain.*;
import com.uagrm.si2g2.asignacion.domain.AsignacionDocenteRepository;
import com.uagrm.si2g2.auth.domain.Rol;
import com.uagrm.si2g2.auth.domain.Usuario;
import com.uagrm.si2g2.auth.domain.UsuarioRepository;
import com.uagrm.si2g2.docente.domain.DocenteRepository;
import com.uagrm.si2g2.docente.domain.Docente;
import com.uagrm.si2g2.estudiante.domain.Estudiante;
import com.uagrm.si2g2.estudiante.domain.EstudianteRepository;
import com.uagrm.si2g2.inscripcion.domain.InscripcionRepository;
import com.uagrm.si2g2.inscripcion.domain.Inscripcion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertaRiesgoServiceTest {
    @Mock private AlertaRiesgoRepository repository;
    @Mock private RecomendacionIaRepository recomendacionRepository;
    @Mock private AlertaRiesgoSeguimientoRepository seguimientoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private DocenteRepository docenteRepository;
    @Mock private AsignacionDocenteRepository asignacionRepository;
    @Mock private InscripcionRepository inscripcionRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @InjectMocks private AlertaRiesgoService service;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordsEffectiveSequentialTransitionWithUserAndObservation() {
        UUID institution = UUID.randomUUID();
        UUID user = authenticate(institution, "DIRECTOR");
        UUID alertId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        AlertaRiesgo alert = AlertaRiesgo.builder().id(alertId).idInstitucion(institution)
                .idEstudiante(studentId).estadoAlerta("EN_SEGUIMIENTO").activa(true).build();
        when(repository.findIdEstudianteByIdAndIdInstitucion(alertId, institution)).thenReturn(Optional.of(studentId));
        when(estudianteRepository.findByIdAndIdInstitucionForUpdate(studentId, institution))
                .thenReturn(Optional.of(Estudiante.builder().id(studentId).codigoEstudiante("E-7")
                        .nombres("Maria").apellidos("Lopez").build()));
        when(repository.findByIdAndIdInstitucionForUpdate(alertId, institution)).thenReturn(Optional.of(alert));
        when(repository.save(alert)).thenReturn(alert);

        var response = service.actualizarEstado(alertId, "atendida", "  Contacto con tutor  ");

        assertEquals("ATENDIDA", response.getEstadoAlerta());
        assertEquals("E-7", response.getCodigoEstudiante());
        assertEquals("Maria Lopez", response.getNombreEstudiante());
        ArgumentCaptor<AlertaRiesgoSeguimiento> captor = ArgumentCaptor.forClass(AlertaRiesgoSeguimiento.class);
        verify(seguimientoRepository).save(captor.capture());
        assertEquals("EN_SEGUIMIENTO", captor.getValue().getEstadoAnterior());
        assertEquals("ATENDIDA", captor.getValue().getEstadoNuevo());
        assertEquals("Contacto con tutor", captor.getValue().getObservacion());
        assertEquals(user, captor.getValue().getIdUsuario());
        InOrder locks = inOrder(repository, estudianteRepository);
        locks.verify(repository).findIdEstudianteByIdAndIdInstitucion(alertId, institution);
        locks.verify(estudianteRepository).findByIdAndIdInstitucionForUpdate(studentId, institution);
        locks.verify(repository).findByIdAndIdInstitucionForUpdate(alertId, institution);
    }

    @Test
    void requiresObservationForAttendedAndClosedStates() {
        authenticate(UUID.randomUUID(), "DIRECTOR");

        assertThrows(IllegalArgumentException.class,
                () -> service.actualizarEstado(UUID.randomUUID(), "ATENDIDA", " "));
        assertThrows(IllegalArgumentException.class,
                () -> service.actualizarEstado(UUID.randomUUID(), "CERRADA", null));
        verifyNoInteractions(repository, seguimientoRepository);
    }

    @Test
    void returnsHistoryInRepositoryOrderWithTenantSafeUser() {
        UUID institution = UUID.randomUUID();
        UUID userId = authenticate(institution, "SECRETARIO");
        UUID alertId = UUID.randomUUID();
        AlertaRiesgo alert = AlertaRiesgo.builder().id(alertId).idInstitucion(institution).build();
        AlertaRiesgoSeguimiento first = AlertaRiesgoSeguimiento.builder().id(UUID.randomUUID())
                .idAlertaRiesgo(alertId).idInstitucion(institution).estadoAnterior("ABIERTA")
                .estadoNuevo("EN_SEGUIMIENTO").idUsuario(userId).creadoEn(Instant.now()).build();
        when(repository.findByIdAndIdInstitucion(alertId, institution)).thenReturn(Optional.of(alert));
        when(seguimientoRepository.findAllByIdAlertaRiesgoAndIdInstitucionOrderByCreadoEnAscIdAsc(alertId, institution))
                .thenReturn(List.of(first));
        when(usuarioRepository.findByIdAndIdInstitucion(userId, institution)).thenReturn(Optional.of(
                Usuario.builder().id(userId).nombres("Ana").apellidos("Perez").build()));

        var result = service.seguimientos(alertId);

        assertEquals(1, result.size());
        assertEquals("Ana Perez", result.getFirst().usuario());
    }

    @Test
    void deniesTeacherWithoutActiveAssignmentOnAlertDetail() {
        UUID institution = UUID.randomUUID();
        UUID userId = authenticate(institution, "DOCENTE");
        UUID alertId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID management = UUID.randomUUID();
        UUID parallel = UUID.randomUUID();
        UUID teacher = UUID.randomUUID();
        AlertaRiesgo alert = AlertaRiesgo.builder().id(alertId).idInstitucion(institution)
                .idEstudiante(studentId).idGestionAcademica(management).build();
        when(repository.findByIdAndIdInstitucion(alertId, institution)).thenReturn(Optional.of(alert));
        when(docenteRepository.findByIdUsuarioAndIdInstitucion(userId, institution)).thenReturn(Optional.of(
                Docente.builder().id(teacher).idInstitucion(institution).build()));
        when(inscripcionRepository.findAllByIdInstitucionAndIdEstudiante(institution, studentId)).thenReturn(List.of(
                Inscripcion.builder().idGestion(management).idParalelo(parallel).estado("ACTIVA").build()));
        when(asignacionRepository.existsByIdInstitucionAndIdDocenteAndIdParaleloAndIdGestionAndEstado(
                institution, teacher, parallel, management, "ACTIVA")).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> service.obtener(alertId, institution));
    }

    @Test
    void listsClosedAlertsWithStudentDataWithoutPerAlertStudentQueries() {
        UUID institution = UUID.randomUUID();
        authenticate(institution, "DIRECTOR");
        UUID studentId = UUID.randomUUID();
        AlertaRiesgo closed = AlertaRiesgo.builder().id(UUID.randomUUID()).idInstitucion(institution)
                .idEstudiante(studentId).estadoAlerta("CERRADA").activa(false).build();
        Estudiante student = Estudiante.builder().id(studentId).idInstitucion(institution)
                .codigoEstudiante("E-10").nombres("Juan").apellidos("Perez").build();
        when(repository.buscarConFiltros(institution, null, null, false)).thenReturn(List.of(closed));
        when(estudianteRepository.findAllByIdInstitucion(institution)).thenReturn(List.of(student));

        var result = service.listar(institution, null, null, false);

        assertEquals(1, result.size());
        assertFalse(result.getFirst().getActiva());
        assertEquals("E-10", result.getFirst().getCodigoEstudiante());
        assertEquals("Juan Perez", result.getFirst().getNombreEstudiante());
        verify(estudianteRepository).findAllByIdInstitucion(institution);
        verify(estudianteRepository, never()).findByIdAndIdInstitucion(any(), any());
    }

    private UUID authenticate(UUID institution, String role) {
        UUID userId = UUID.randomUUID();
        Usuario user = Usuario.builder().id(userId).idInstitucion(institution).correo("test@example.com")
                .hashContrasena("secret").nombres("Test").apellidos("User")
                .roles(Set.of(Rol.builder().codigo(role).nombre(role).esGlobal(true).build())).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        return userId;
    }
}
