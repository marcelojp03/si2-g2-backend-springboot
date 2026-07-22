package com.uagrm.si2g2.alertas.application;

import com.uagrm.si2g2.academico.domain.GestionAcademica;
import com.uagrm.si2g2.academico.domain.GestionAcademicaRepository;
import com.uagrm.si2g2.institucion.domain.Institucion;
import com.uagrm.si2g2.institucion.domain.InstitucionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RiesgoAcademicoSchedulerTest {
    @Mock private InstitucionRepository institucionRepository;
    @Mock private GestionAcademicaRepository gestionRepository;
    @Mock private RiesgoAcademicoTenantProcessor tenantProcessor;
    @InjectMocks private RiesgoAcademicoScheduler scheduler;

    @Test
    void processesAllActiveTenantsAndIsolatesFailures() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID firstManagement = UUID.randomUUID();
        UUID secondManagement = UUID.randomUUID();
        when(institucionRepository.findAllByEstado("ACTIVO")).thenReturn(List.of(
                Institucion.builder().id(first).build(), Institucion.builder().id(second).build()));
        when(gestionRepository.findByIdInstitucionAndActivaTrue(first)).thenReturn(Optional.of(
                GestionAcademica.builder().id(firstManagement).idInstitucion(first).activa(true).build()));
        when(gestionRepository.findByIdInstitucionAndActivaTrue(second)).thenReturn(Optional.of(
                GestionAcademica.builder().id(secondManagement).idInstitucion(second).activa(true).build()));
        doThrow(new IllegalStateException("tenant failure")).when(tenantProcessor).procesar(first, firstManagement);

        scheduler.ejecutar();

        verify(tenantProcessor).procesar(first, firstManagement);
        verify(tenantProcessor).procesar(second, secondManagement);
    }
}
