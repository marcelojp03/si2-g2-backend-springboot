package com.uagrm.si2g2.seed.application;

import com.uagrm.si2g2.seed.dto.SeedResult;
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

    private final SyntheticDataSeeder syntheticDataSeeder;

    @Override
    public void run(ApplicationArguments args) {
        SeedResult result = syntheticDataSeeder.seed();
        log.info(
                "Datos sinteticos inicializados: instituciones={}, creados={}, existentes={}",
                result.institucionCodigo(),
                result.creados(),
                result.existentes()
        );
    }
}
