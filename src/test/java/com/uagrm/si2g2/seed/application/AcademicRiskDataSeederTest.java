package com.uagrm.si2g2.seed.application;

import com.uagrm.si2g2.academico.domain.GestionAcademica;
import com.uagrm.si2g2.academico.domain.GestionAcademicaRepository;
import com.uagrm.si2g2.institucion.domain.Institucion;
import com.uagrm.si2g2.institucion.domain.InstitucionRepository;
import com.uagrm.si2g2.seed.dto.AcademicRiskSeedResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcademicRiskDataSeederTest {

    @Mock
    private InstitucionRepository institucionRepository;

    @Mock
    private GestionAcademicaRepository gestionRepository;

    @Mock
    private NamedParameterJdbcTemplate jdbc;

    @InjectMocks
    private AcademicRiskDataSeeder seeder;

    @Test
    void shouldSeedEvaluationsPerTeachingAssignmentAndReportCounts() {
        UUID institutionId = UUID.randomUUID();
        Institucion institution = Institucion.builder().id(institutionId).codigo("CSM-001").build();
        GestionAcademica management = GestionAcademica.builder()
                .id(UUID.randomUUID())
                .idInstitucion(institutionId)
                .fechaInicio(LocalDate.of(2026, 2, 1))
                .fechaFin(LocalDate.of(2026, 11, 30))
                .activa(true)
                .build();
        Map<String, Integer> profiles = new LinkedHashMap<>();
        profiles.put("BAJO", 240);
        profiles.put("MEDIO", 96);
        profiles.put("ALTO", 72);
        profiles.put("CRITICO", 72);
        profiles.put("SIN_DATOS", 0);

        when(institucionRepository.findByCodigo("CSM-001")).thenReturn(Optional.of(institution));
        when(gestionRepository.findByIdInstitucionAndActivaTrue(institutionId)).thenReturn(Optional.of(management));
        when(jdbc.queryForObject(anyString(), any(SqlParameterSource.class),
                org.mockito.ArgumentMatchers.<RowMapper<Map<String, Integer>>>any())).thenReturn(profiles);
        when(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), eq(Integer.class)))
                .thenReturn(3, 1440, 28800, 2400, 48000);

        AcademicRiskSeedResult result = seeder.seed(" csm-001 ");

        assertEquals("CSM-001", result.institutionCode());
        assertEquals(1440, result.evaluaciones());
        assertEquals(28800, result.calificaciones());
        assertEquals(profiles, result.perfilesEsperados());

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc, times(13)).update(sql.capture(), any(SqlParameterSource.class));
        String evaluationInsert = sql.getAllValues().stream()
                .filter(value -> value.contains("INSERT INTO sia.evaluacion"))
                .findFirst().orElseThrow();
        assertTrue(evaluationInsert.contains("SELECT a.id, a.id_materia"));
        assertFalse(evaluationInsert.contains("DISTINCT ON"));
        assertTrue(evaluationInsert.contains("id_asignacion_docente"));
        assertTrue(evaluationInsert.contains("ON CONFLICT (id_asignacion_docente, periodo, nombre)"));
        String gradeInsert = sql.getAllValues().stream()
                .filter(value -> value.contains("INSERT INTO sia.calificacion"))
                .findFirst().orElseThrow();
        assertTrue(gradeInsert.contains("ev.periodo IN (1, 2, 3)"));
        assertTrue(gradeInsert.contains("ev.id_asignacion_docente = ad.id"));
        assertTrue(gradeInsert.contains("removed_cross_parallel_grades"));
        assertTrue(gradeInsert.contains("PARTITION BY evaluation_id"));
        assertTrue(gradeInsert.contains("provisional_grade * 0.85"));
    }
}
