package com.uagrm.si2g2.seed.application;

import com.uagrm.si2g2.institucion.domain.Institucion;
import com.uagrm.si2g2.institucion.domain.InstitucionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(100)
@RequiredArgsConstructor
public class SyntheticSeedInitializer implements ApplicationRunner {

    private final InstitucionRepository institucionRepository;

    @Override
    public void run(ApplicationArguments args) {
        long institucionesConDatos = institucionRepository.findAll().stream()
                .filter(inst -> inst.getCodigo() != null)
                .count();
        log.info("Seed automatico omitido. {} instituciones disponibles con datos ya generados.", institucionesConDatos);
        log.info("Usa POST /api/seed/synthetic (SUPER_ADMIN) para regenerar datos manualmente.");
    }
}
