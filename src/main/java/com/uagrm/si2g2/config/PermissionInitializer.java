package com.uagrm.si2g2.config;

import com.uagrm.si2g2.auth.application.PermissionCatalog;
import com.uagrm.si2g2.auth.domain.Permiso;
import com.uagrm.si2g2.auth.domain.PermisoRepository;
import com.uagrm.si2g2.auth.domain.RolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class PermissionInitializer implements ApplicationRunner {

    private final PermisoRepository permisoRepository;
    private final RolRepository rolRepository;

    private static final Set<String> OBSOLETE_PERMISSIONS = Set.of(
            "USUARIOS_WRITE", "CONFIGURACION_WRITE", "GESTION_WRITE",
            "PERSONAS_WRITE", "OPERACION_WRITE", "ROLES_WRITE",
            "ASISTENCIA_WRITE", "CALIFICACIONES_WRITE",
            "GESTION_CREATE", "GESTION_UPDATE", "GESTION_DELETE", "GESTION_READ",
            "PERSONAS_CREATE", "PERSONAS_UPDATE", "PERSONAS_DELETE", "PERSONAS_READ",
            "OPERACION_CREATE", "OPERACION_UPDATE", "OPERACION_DELETE", "OPERACION_READ"
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("=== PermissionInitializer: INICIANDO sincronizacion ===");
        cleanObsoletePermissions();
        createOrupdatePermissions();
        syncBaseRolePermissions();
        log.info("=== PermissionInitializer: COMPLETADO ===");
    }

    private void cleanObsoletePermissions() {
        int deleted = 0;
        for (String codigo : OBSOLETE_PERMISSIONS) {
            var opt = permisoRepository.findByCodigo(codigo);
            if (opt.isPresent()) {
                Permiso permiso = opt.get();
                var roles = rolRepository.findAll();
                for (var rol : roles) {
                    if (rol.getPermisos() != null && rol.getPermisos().removeIf(p -> p.getId().equals(permiso.getId()))) {
                        rolRepository.save(rol);
                    }
                }
                permisoRepository.delete(permiso);
                deleted++;
                log.info("Permiso obsoleto eliminado: {}", codigo);
            }
        }
        log.info("Total permisos obsoletos eliminados: {}", deleted);
    }

    private void createOrupdatePermissions() {
        int created = 0;
        int updated = 0;
        for (var def : PermissionCatalog.DEFINITIONS) {
            Permiso existing = permisoRepository.findByCodigo(def.getCodigo()).orElse(null);
            if (existing != null) {
                boolean changed = false;
                if (!def.getNombre().equals(existing.getNombre())) {
                    existing.setNombre(def.getNombre());
                    changed = true;
                }
                if (!def.getDescripcion().equals(existing.getDescripcion())) {
                    existing.setDescripcion(def.getDescripcion());
                    changed = true;
                }
                if (!def.getModulo().equals(existing.getModulo())) {
                    existing.setModulo(def.getModulo());
                    changed = true;
                }
                if (!def.getAccion().equals(existing.getAccion())) {
                    existing.setAccion(def.getAccion());
                    changed = true;
                }
                if (changed) {
                    permisoRepository.save(existing);
                    updated++;
                    log.info("Permiso actualizado: {}", existing.getCodigo());
                }
            } else {
                Permiso permiso = Permiso.builder()
                        .codigo(def.getCodigo())
                        .nombre(def.getNombre())
                        .modulo(def.getModulo())
                        .accion(def.getAccion())
                        .descripcion(def.getDescripcion())
                        .build();
                permisoRepository.save(permiso);
                created++;
                log.info("Permiso creado: {}", permiso.getCodigo());
            }
        }
        log.info("Permisos creados: {}, actualizados: {}", created, updated);
    }

    private void syncBaseRolePermissions() {
        Map<String, Permiso> permisos = PermissionCatalog.DEFINITIONS.stream()
                .map(def -> permisoRepository.findByCodigo(def.getCodigo()).orElseThrow())
                .collect(Collectors.toMap(Permiso::getCodigo, p -> p));

        syncRole("ADMIN_INSTITUCION", PermissionCatalog.ADMIN_INSTITUCION, permisos);
        syncRole("DIRECTOR", PermissionCatalog.DIRECTOR, permisos);
        syncRole("SECRETARIO", PermissionCatalog.SECRETARIO, permisos);
        syncRole("DOCENTE", PermissionCatalog.DOCENTE, permisos);
        syncRole("ESTUDIANTE", PermissionCatalog.ESTUDIANTE, permisos);
        syncRole("TUTOR", PermissionCatalog.TUTOR, permisos);
    }

    private void syncRole(String roleCode, Set<String> permissionCodes, Map<String, Permiso> permisos) {
        rolRepository.findByCodigo(roleCode).ifPresent(rol -> {
            rol.setEsGlobal(true);
            rol.setIdInstitucion(null);
            Set<Permiso> desired = permissionCodes.stream()
                    .map(permisos::get)
                    .collect(Collectors.toCollection(HashSet::new));

            if (!desired.equals(new HashSet<>(rol.getPermisos()))) {
                rol.setPermisos(desired);
                rolRepository.save(rol);
                log.info("Rol {} sincronizado con {} permisos", roleCode, desired.size());
            }
        });
    }
}
