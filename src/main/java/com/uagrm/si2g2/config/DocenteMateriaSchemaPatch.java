package com.uagrm.si2g2.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(5)
@Profile("!test")
@RequiredArgsConstructor
public class DocenteMateriaSchemaPatch implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS docente_materia (
                    id_docente UUID NOT NULL REFERENCES docente(id) ON DELETE CASCADE,
                    id_materia UUID NOT NULL REFERENCES materia(id) ON DELETE CASCADE,
                    PRIMARY KEY (id_docente, id_materia)
                )
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_docente_materia_materia ON docente_materia (id_materia)
                """);
        log.info("Tabla docente_materia verificada");
    }
}
