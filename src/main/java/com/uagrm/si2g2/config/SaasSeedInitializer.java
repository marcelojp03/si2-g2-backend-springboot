package com.uagrm.si2g2.config;

import com.uagrm.si2g2.saas.plan.domain.ModuloSistema;
import com.uagrm.si2g2.saas.plan.domain.ModuloSistemaRepository;
import com.uagrm.si2g2.saas.plan.domain.PlanSuscripcion;
import com.uagrm.si2g2.saas.plan.domain.PlanSuscripcionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Seed de módulos del sistema y planes de suscripción (Sprint Especial SaaS).
 * Solo inserta si los datos no existen aún.
 */
@Slf4j
@Component
@Order(30)
@RequiredArgsConstructor
public class SaasSeedInitializer implements ApplicationRunner {

    private final ModuloSistemaRepository moduloRepo;
    private final PlanSuscripcionRepository planRepo;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedModulos();
        seedPlanes();
    }

    private void seedModulos() {
        List<Object[]> definiciones = List.of(
                // {codigo, nombre, descripcion, icono, rutaFrontend, ordenVisual}
                new Object[]{"ACADEMICO_BASE", "Académico Base", "Gestión académica central: gestiones, cursos, materias, docentes, estudiantes, tutores, inscripciones", "pi pi-book", "/sia/cursos", 1},
                new Object[]{"AULAS", "Aulas", "Gestión de aulas y recursos físicos", "pi pi-building", "/sia/aulas", 2},
                new Object[]{"HORARIOS", "Horarios", "Planificación de horarios de clases", "pi pi-calendar", "/sia/horarios", 3},
                new Object[]{"ASISTENCIA", "Asistencia", "Registro y seguimiento de asistencia estudiantil", "pi pi-check-square", "/sia/asistencia", 4},
                new Object[]{"CALIFICACIONES", "Calificaciones", "Registro de evaluaciones y calificaciones", "pi pi-star", "/sia/calificaciones", 5},
                new Object[]{"REPORTES", "Reportes", "Reportes académicos configurables y exportación", "pi pi-chart-bar", "/sia/reportes", 6},
                new Object[]{"SAAS_GESTION", "Gestión SaaS", "Administración de planes, suscripciones y facturación", "pi pi-credit-card", "/admin/saas", 7},
                new Object[]{"SEGURIDAD", "Seguridad", "Monitoreo de accesos, intentos de login y bitácora de auditoría", "pi pi-shield", "/admin/seguridad", 8},
                new Object[]{"RESPALDO", "Respaldo y Restauración", "Gestión de respaldos y restauraciones del sistema", "pi pi-database", "/admin/respaldo", 9}
        );

        for (Object[] def : definiciones) {
            String codigo = (String) def[0];
            if (moduloRepo.existsByCodigo(codigo)) continue;

            ModuloSistema modulo = new ModuloSistema();
            modulo.setCodigo(codigo);
            modulo.setNombre((String) def[1]);
            modulo.setDescripcion((String) def[2]);
            modulo.setIcono((String) def[3]);
            modulo.setRutaFrontend((String) def[4]);
            modulo.setOrdenVisual((Integer) def[5]);
            modulo.setEstado("ACTIVO");
            moduloRepo.save(modulo);
            log.info("Módulo creado: {}", codigo);
        }
    }

    private void seedPlanes() {
        if (planRepo.existsByCodigo("BASICO") && planRepo.existsByCodigo("PROFESIONAL") && planRepo.existsByCodigo("EMPRESARIAL")) {
            log.info("Planes SaaS ya existen, omitiendo seed.");
            return;
        }

        // Recuperar módulos necesarios
        ModuloSistema academicoBase = moduloRepo.findByCodigo("ACADEMICO_BASE").orElse(null);
        ModuloSistema aulas = moduloRepo.findByCodigo("AULAS").orElse(null);
        ModuloSistema horarios = moduloRepo.findByCodigo("HORARIOS").orElse(null);
        ModuloSistema asistencia = moduloRepo.findByCodigo("ASISTENCIA").orElse(null);
        ModuloSistema calificaciones = moduloRepo.findByCodigo("CALIFICACIONES").orElse(null);
        ModuloSistema reportes = moduloRepo.findByCodigo("REPORTES").orElse(null);
        ModuloSistema saasGestion = moduloRepo.findByCodigo("SAAS_GESTION").orElse(null);
        ModuloSistema seguridad = moduloRepo.findByCodigo("SEGURIDAD").orElse(null);
        ModuloSistema respaldo = moduloRepo.findByCodigo("RESPALDO").orElse(null);

        // Plan BÁSICO: módulos esenciales, 10 usuarios
        if (!planRepo.existsByCodigo("BASICO")) {
            PlanSuscripcion basico = new PlanSuscripcion();
            basico.setCodigo("BASICO");
            basico.setNombre("Plan Básico");
            basico.setDescripcion("Módulos académicos esenciales para instituciones pequeñas");
            basico.setMaxUsuarios(10);
            basico.setMaxAlmacenamientoMb(1024);
            basico.setPrecioMensual(new BigDecimal("99.00"));
            basico.setEstado("ACTIVO");
            Set<ModuloSistema> modBasico = new java.util.HashSet<>();
            if (academicoBase != null) modBasico.add(academicoBase);
            if (aulas != null) modBasico.add(aulas);
            basico.setModulos(modBasico);
            planRepo.save(basico);
            log.info("Plan BASICO creado");
        }

        // Plan PROFESIONAL: todos los módulos académicos, 50 usuarios
        if (!planRepo.existsByCodigo("PROFESIONAL")) {
            PlanSuscripcion profesional = new PlanSuscripcion();
            profesional.setCodigo("PROFESIONAL");
            profesional.setNombre("Plan Profesional");
            profesional.setDescripcion("Suite académica completa con asistencia, calificaciones y reportes");
            profesional.setMaxUsuarios(50);
            profesional.setMaxAlmacenamientoMb(10240);
            profesional.setPrecioMensual(new BigDecimal("299.00"));
            profesional.setEstado("ACTIVO");
            Set<ModuloSistema> modPro = new java.util.HashSet<>();
            if (academicoBase != null) modPro.add(academicoBase);
            if (aulas != null) modPro.add(aulas);
            if (horarios != null) modPro.add(horarios);
            if (asistencia != null) modPro.add(asistencia);
            if (calificaciones != null) modPro.add(calificaciones);
            if (reportes != null) modPro.add(reportes);
            profesional.setModulos(modPro);
            planRepo.save(profesional);
            log.info("Plan PROFESIONAL creado");
        }

        // Plan EMPRESARIAL: todos los módulos, usuarios ilimitados (Integer.MAX_VALUE)
        if (!planRepo.existsByCodigo("EMPRESARIAL")) {
            PlanSuscripcion empresarial = new PlanSuscripcion();
            empresarial.setCodigo("EMPRESARIAL");
            empresarial.setNombre("Plan Empresarial");
            empresarial.setDescripcion("Plataforma completa con SaaS, seguridad avanzada y respaldo");
            empresarial.setMaxUsuarios(Integer.MAX_VALUE);
            empresarial.setMaxAlmacenamientoMb(102400);
            empresarial.setPrecioMensual(new BigDecimal("799.00"));
            empresarial.setEstado("ACTIVO");
            Set<ModuloSistema> modEmp = new java.util.HashSet<>();
            if (academicoBase != null) modEmp.add(academicoBase);
            if (aulas != null) modEmp.add(aulas);
            if (horarios != null) modEmp.add(horarios);
            if (asistencia != null) modEmp.add(asistencia);
            if (calificaciones != null) modEmp.add(calificaciones);
            if (reportes != null) modEmp.add(reportes);
            if (saasGestion != null) modEmp.add(saasGestion);
            if (seguridad != null) modEmp.add(seguridad);
            if (respaldo != null) modEmp.add(respaldo);
            empresarial.setModulos(modEmp);
            planRepo.save(empresarial);
            log.info("Plan EMPRESARIAL creado");
        }
    }
}
