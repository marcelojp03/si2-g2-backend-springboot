package com.uagrm.si2g2.notificacion.application;

import com.uagrm.si2g2.notificacion.domain.Notificacion;
import com.uagrm.si2g2.notificacion.domain.NotificacionRepository;
import com.uagrm.si2g2.notificacion.domain.SesionDispositivo;
import com.uagrm.si2g2.notificacion.domain.SesionDispositivoRepository;
import com.uagrm.si2g2.notificacion.dto.NotificacionResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository repository;
    private final SesionDispositivoRepository sesionRepository;
    private final FcmService fcmService;

    @Transactional
    public NotificacionResponse crearParaUsuario(UUID idInstitucion, UUID idUsuario,
                                                  String titulo, String mensaje, String tipo,
                                                  String referenciaTipo, UUID referenciaId) {
        Notificacion n = Notificacion.builder()
                .idInstitucion(idInstitucion)
                .idUsuario(idUsuario)
                .titulo(titulo)
                .mensaje(mensaje)
                .tipo(tipo != null ? tipo : "SISTEMA")
                .referenciaTipo(referenciaTipo)
                .referenciaId(referenciaId)
                .build();
        return NotificacionResponse.from(repository.save(n));
    }

    @Transactional
    public void crearMasiva(UUID idInstitucion, List<UUID> idsUsuarios,
                            String titulo, String mensaje, String tipo,
                            String referenciaTipo, UUID referenciaId) {
        for (UUID idUsuario : idsUsuarios) {
            repository.save(Notificacion.builder()
                    .idInstitucion(idInstitucion)
                    .idUsuario(idUsuario)
                    .titulo(titulo)
                    .mensaje(mensaje)
                    .tipo(tipo != null ? tipo : "SISTEMA")
                    .referenciaTipo(referenciaTipo)
                    .referenciaId(referenciaId)
                    .build());
        }
    }

    @Transactional(readOnly = true)
    public List<NotificacionResponse> listarNoLeidas(UUID idUsuario) {
        return repository.findByIdUsuarioAndLeidaFalseOrderByCreadoEnDesc(idUsuario)
                .stream().map(NotificacionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<NotificacionResponse> listar(UUID idUsuario, int page, int size) {
        return repository.findByIdUsuarioOrderByCreadoEnDesc(idUsuario, PageRequest.of(page, Math.min(size, 100)))
                .stream().map(NotificacionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public long contarNoLeidas(UUID idUsuario) {
        return repository.countByIdUsuarioAndLeidaFalse(idUsuario);
    }

    @Transactional
    public void marcarLeida(UUID id) {
        Notificacion n = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Notificacion no encontrada: " + id));
        n.setLeida(true);
        n.setLeidaEn(Instant.now());
        repository.save(n);
    }

    @Transactional
    public int marcarTodasLeidas(UUID idUsuario) {
        return repository.marcarTodasLeidas(idUsuario);
    }

    @Transactional
    public SesionDispositivo registrarDispositivo(UUID idInstitucion, UUID idUsuario,
                                                   String token, String plataforma) {
        SesionDispositivo sd = SesionDispositivo.builder()
                .idInstitucion(idInstitucion)
                .idUsuario(idUsuario)
                .tokenDispositivo(token)
                .plataforma(plataforma.toUpperCase())
                .build();
        return sesionRepository.save(sd);
    }
}
