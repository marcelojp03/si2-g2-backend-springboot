package com.uagrm.si2g2.config;

import com.uagrm.si2g2.persona.application.PersonaProvisioningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
public class PersonaProfileInitializer implements ApplicationRunner {

    private final PersonaProvisioningService personaProvisioningService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        personaProvisioningService.syncMissingProfiles();
        log.info("Perfiles académicos sincronizados con usuarios existentes");
    }
}
