package com.uagrm.si2g2.config;

import com.uagrm.si2g2.auth.application.PermissionCatalog;
import com.uagrm.si2g2.auth.domain.Permiso;
import com.uagrm.si2g2.auth.domain.PermisoRepository;
import com.uagrm.si2g2.auth.domain.Rol;
import com.uagrm.si2g2.auth.domain.RolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionInitializer implements ApplicationRunner {

    private final PermisoRepository permisoRepository;
    private final RolRepository rolRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Map<String, Permiso> permisos = ensurePermissionCatalog();
        assignBaseRolePermissions("ADMIN_INSTITUCION", PermissionCatalog.ADMIN_INSTITUCION, permisos);
        assignBaseRolePermissions("DIRECTOR", PermissionCatalog.DIRECTOR, permisos);
        assignBaseRolePermissions("SECRETARIO", PermissionCatalog.SECRETARIO, permisos);
        assignBaseRolePermissions("DOCENTE", PermissionCatalog.DOCENTE, permisos);
        assignBaseRolePermissions("ESTUDIANTE", PermissionCatalog.ESTUDIANTE, permisos);
        assignBaseRolePermissions("TUTOR", PermissionCatalog.TUTOR, permisos);
    }

    private Map<String, Permiso> ensurePermissionCatalog() {
        return PermissionCatalog.DEFINITIONS.stream()
                .map(def -> permisoRepository.findByCodigo(def.getCodigo())
                        .orElseGet(createPermission(def)))
                .collect(Collectors.toMap(Permiso::getCodigo, permiso -> permiso));
    }

    private Supplier<Permiso> createPermission(PermissionCatalog.Definition definition) {
        return () -> {
            Permiso permiso = Permiso.builder()
                    .codigo(definition.getCodigo())
                    .nombre(definition.getNombre())
                    .modulo(definition.getModulo())
                    .accion(definition.getAccion())
                    .descripcion(definition.getDescripcion())
                    .build();
            log.info("Permiso base creado: {}", permiso.getCodigo());
            return permisoRepository.save(permiso);
        };
    }

    private void assignBaseRolePermissions(String roleCode, Set<String> permissionCodes, Map<String, Permiso> permisos) {
        rolRepository.findByCodigo(roleCode).ifPresent(rol -> {
            rol.setEsGlobal(true);
            rol.setIdInstitucion(null);
            Set<Permiso> desired = permissionCodes.stream()
                    .map(permisos::get)
                    .collect(Collectors.toCollection(HashSet::new));

            if (!rol.getPermisos().equals(desired)) {
                rol.setPermisos(desired);
                rolRepository.save(rol);
                log.info("Permisos sincronizados para rol base: {}", roleCode);
            }
        });
    }
}
