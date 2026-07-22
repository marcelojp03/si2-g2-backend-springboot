package com.uagrm.si2g2.alertas.application;

import com.uagrm.si2g2.curso.domain.Paralelo;
import com.uagrm.si2g2.curso.domain.ParaleloRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RiesgoAcademicoTenantProcessorTest {
    @Mock private ParaleloRepository paraleloRepository;
    @Mock private RiesgoAcademicoParaleloProcessor paraleloProcessor;
    @InjectMocks private RiesgoAcademicoTenantProcessor tenantProcessor;

    @Test
    void continuesWithNextParallelWhenOneFails() {
        UUID institution = UUID.randomUUID();
        UUID management = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(paraleloRepository.findAllByIdInstitucionAndIdGestionAcademica(institution, management))
                .thenReturn(List.of(Paralelo.builder().id(first).estado("ACTIVO").build(),
                        Paralelo.builder().id(second).estado("ACTIVO").build()));
        doThrow(new IllegalStateException("parallel failure"))
                .when(paraleloProcessor).procesar(institution, first, management);

        tenantProcessor.procesar(institution, management);

        verify(paraleloProcessor).procesar(institution, first, management);
        verify(paraleloProcessor).procesar(institution, second, management);
    }

    @Test
    void parallelProcessorUsesRequiresNewTransaction() throws Exception {
        Transactional transactional = RiesgoAcademicoParaleloProcessor.class
                .getMethod("procesar", UUID.class, UUID.class, UUID.class)
                .getAnnotation(Transactional.class);

        assertNotNull(transactional);
        assertEquals(Propagation.REQUIRES_NEW, transactional.propagation());
    }
}
