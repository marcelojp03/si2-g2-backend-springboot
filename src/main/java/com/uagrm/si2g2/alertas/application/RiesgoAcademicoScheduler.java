package com.uagrm.si2g2.alertas.application;

import com.uagrm.si2g2.academico.domain.GestionAcademicaRepository;
import com.uagrm.si2g2.institucion.domain.Institucion;
import com.uagrm.si2g2.institucion.domain.InstitucionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "riesgo-academico.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RiesgoAcademicoScheduler {
    private final InstitucionRepository institucionRepository;
    private final GestionAcademicaRepository gestionRepository;
    private final RiesgoAcademicoTenantProcessor tenantProcessor;
    private final AtomicBoolean ejecutando = new AtomicBoolean(false);

    @Scheduled(cron = "${riesgo-academico.scheduler.cron:0 0 2 * * *}")
    public void ejecutar() {
        if (!ejecutando.compareAndSet(false, true)) {
            log.warn("Analisis nocturno de riesgo omitido: existe una ejecucion en curso");
            return;
        }
        log.info("Inicio del analisis nocturno de riesgo academico");
        int procesadas = 0;
        try {
            for (Institucion institucion : institucionRepository.findAllByEstado("ACTIVO")) {
                var gestion = gestionRepository.findByIdInstitucionAndActivaTrue(institucion.getId());
                if (gestion.isEmpty()) {
                    log.info("Analisis de riesgo omitido para institucion {}: sin gestion activa", institucion.getId());
                    continue;
                }
                try {
                    tenantProcessor.procesar(institucion.getId(), gestion.get().getId());
                    procesadas++;
                    log.info("Analisis de riesgo finalizado para institucion {} y gestion {}",
                            institucion.getId(), gestion.get().getId());
                } catch (Exception exception) {
                    log.error("Error en analisis de riesgo para institucion {} y gestion {}",
                            institucion.getId(), gestion.get().getId(), exception);
                }
            }
        } finally {
            ejecutando.set(false);
            log.info("Fin del analisis nocturno de riesgo academico: {} instituciones procesadas", procesadas);
        }
    }
}
