package com.uagrm.si2g2.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Parches idempotentes de esquema requeridos por el Sprint Especial antes de los seeders.
 */
@Slf4j
@Component
@Order(1)
@Profile("!test")
@RequiredArgsConstructor
public class SprintEspecialSchemaPatch implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("ALTER TABLE sia.usuario ADD COLUMN IF NOT EXISTS fcm_token TEXT NULL");
        log.info("Columna usuario.fcm_token verificada");
    }
}
