package com.uagrm.si2g2.comunicado.application;

import com.uagrm.si2g2.auditoria.application.AuditoriaService;
import com.uagrm.si2g2.auth.domain.UsuarioRepository;
import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.comunicado.domain.Comunicado;
import com.uagrm.si2g2.comunicado.domain.ComunicadoRepository;
import com.uagrm.si2g2.comunicado.dto.ComunicadoRequest;
import com.uagrm.si2g2.comunicado.dto.ComunicadoResponse;
import com.uagrm.si2g2.notificacion.application.FcmService;
import com.uagrm.si2g2.notificacion.application.NotificacionService;
import com.uagrm.si2g2.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComunicadoService {

    private final ComunicadoRepository repository;
    private final AuditoriaService auditoriaService;
    private final NotificacionService notificacionService;
    private final FcmService fcmService;
    private final UsuarioRepository usuarioRepository;

    private static final int MAX_LIMITE = 200;

    @Transactional
    public ComunicadoResponse crear(ComunicadoRequest request) {
        UUID idInstitucion = TenantContext.get();
        UUID userId = SecurityUtils.requireCurrentInstitutionId();
        UUID currentUserId = SecurityUtils.currentUserId();

        Comunicado comunicado = Comunicado.builder()
                .idInstitucion(idInstitucion)
                .titulo(request.getTitulo().trim())
                .contenido(request.getContenido().trim())
                .tipo(request.getTipo() != null ? request.getTipo() : "AVISO")
                .destinatarios(request.getDestinatarios() != null ? request.getDestinatarios() : "TODOS")
                .creadoPor(currentUserId)
                .build();

        ComunicadoResponse resp = ComunicadoResponse.from(repository.save(comunicado));
        auditoriaService.registrar(idInstitucion, currentUserId,
                "COMUNICADO", "CREAR", "comunicado", resp.getId().toString(),
                true, "Comunicado creado: " + resp.getTitulo());
        return resp;
    }

    @Transactional(readOnly = true)
    public List<ComunicadoResponse> listar(String estado, String tipo, int page, int size) {
        int limite = Math.min(size, MAX_LIMITE);
        return repository.buscarConFiltros(TenantContext.get(), estado, tipo, PageRequest.of(page, limite))
                .stream().map(ComunicadoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ComunicadoResponse> listarPublicados(String tipo, int page, int size) {
        int limite = Math.min(size, MAX_LIMITE);
        return repository.findPublicados(TenantContext.get(), tipo, PageRequest.of(page, limite))
                .stream().map(ComunicadoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ComunicadoResponse obtener(UUID id) {
        return ComunicadoResponse.from(buscar(id));
    }

    @Transactional
    public ComunicadoResponse actualizar(UUID id, ComunicadoRequest request) {
        Comunicado c = buscar(id);
        if (!"BORRADOR".equals(c.getEstado())) {
            throw new IllegalStateException("Solo se puede editar un comunicado en estado BORRADOR");
        }
        c.setTitulo(request.getTitulo().trim());
        c.setContenido(request.getContenido().trim());
        c.setTipo(request.getTipo() != null ? request.getTipo() : "AVISO");
        c.setDestinatarios(request.getDestinatarios() != null ? request.getDestinatarios() : "TODOS");
        ComunicadoResponse resp = ComunicadoResponse.from(repository.save(c));
        auditoriaService.registrar(TenantContext.get(), SecurityUtils.currentUserId(),
                "COMUNICADO", "ACTUALIZAR", "comunicado", id.toString(),
                true, "Comunicado actualizado: " + resp.getTitulo());
        return resp;
    }

    @Transactional
    public ComunicadoResponse publicar(UUID id) {
        Comunicado c = buscar(id);
        if (!"BORRADOR".equals(c.getEstado())) {
            throw new IllegalStateException("El comunicado ya fue publicado o archivado");
        }
        c.setEstado("PUBLICADO");
        c.setPublicadoEn(Instant.now());
        c.setPublicadoPor(SecurityUtils.currentUserId());
        ComunicadoResponse resp = ComunicadoResponse.from(repository.save(c));
        auditoriaService.registrar(TenantContext.get(), SecurityUtils.currentUserId(),
                "COMUNICADO", "PUBLICAR", "comunicado", id.toString(),
                true, "Comunicado publicado: " + resp.getTitulo());

        // Notificar a los usuarios destinatarios
        notificarDestinatarios(c);
        return resp;
    }

    private void notificarDestinatarios(Comunicado c) {
        try {
            var rolesPorDestinatario = java.util.Map.of(
                    "TODOS", List.of("DOCENTE", "ESTUDIANTE", "TUTOR", "ADMIN_INSTITUCION", "DIRECTOR", "SECRETARIO"),
                    "DOCENTES", List.of("DOCENTE"),
                    "ESTUDIANTES", List.of("ESTUDIANTE"),
                    "TUTORES", List.of("TUTOR"),
                    "ADMINISTRATIVOS", List.of("ADMIN_INSTITUCION", "DIRECTOR", "SECRETARIO")
            );
            var roles = rolesPorDestinatario.getOrDefault(c.getDestinatarios(), List.of("DOCENTE", "ESTUDIANTE"));
            var usuarios = usuarioRepository.findByIdInstitucionAndRoles(c.getIdInstitucion(), roles);
            var fcmTokens = new java.util.ArrayList<String>();
            for (var u : usuarios) {
                notificacionService.crearParaUsuario(
                        c.getIdInstitucion(), u.getId(),
                        c.getTitulo(),
                        c.getContenido().length() > 200 ? c.getContenido().substring(0, 200) + "..." : c.getContenido(),
                        "COMUNICADO", "comunicado", c.getId());
                if (u.getFcmToken() != null && !u.getFcmToken().isBlank()) {
                    fcmTokens.add(u.getFcmToken());
                }
            }
            if (!fcmTokens.isEmpty()) {
                var contenido = c.getContenido().length() > 200
                        ? c.getContenido().substring(0, 200) + "..."
                        : c.getContenido();
                fcmService.enviarAMultiplesDispositivos(
                        fcmTokens, c.getTitulo(), contenido,
                        java.util.Map.of("tipo", "COMUNICADO", "id", c.getId().toString()));
            }
        } catch (Exception e) {
            log.warn("Error al notificar destinatarios del comunicado {}: {}", c.getId(), e.getMessage());
        }
    }

    @Transactional
    public ComunicadoResponse archivar(UUID id) {
        Comunicado c = buscar(id);
        if ("ARCHIVADO".equals(c.getEstado())) {
            throw new IllegalStateException("El comunicado ya esta archivado");
        }
        c.setEstado("ARCHIVADO");
        ComunicadoResponse resp = ComunicadoResponse.from(repository.save(c));
        auditoriaService.registrar(TenantContext.get(), SecurityUtils.currentUserId(),
                "COMUNICADO", "ARCHIVAR", "comunicado", id.toString(),
                true, "Comunicado archivado: " + resp.getTitulo());
        return resp;
    }

    private Comunicado buscar(UUID id) {
        return repository.findByIdAndIdInstitucion(id, TenantContext.get())
                .orElseThrow(() -> new EntityNotFoundException("Comunicado no encontrado: " + id));
    }
}
