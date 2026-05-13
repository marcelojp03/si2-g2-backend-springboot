package com.uagrm.si2g2.institucion.application;

import com.uagrm.si2g2.auditoria.application.AuditoriaService;
import com.uagrm.si2g2.institucion.domain.ConfiguracionInstitucionRepository;
import com.uagrm.si2g2.institucion.domain.Institucion;
import com.uagrm.si2g2.institucion.domain.InstitucionRepository;
import com.uagrm.si2g2.institucion.dto.ConfiguracionInstitucionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfiguracionServiceTest {

    @Mock
    private ConfiguracionInstitucionRepository configuracionRepository;

    @Mock
    private InstitucionRepository institucionRepository;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private ConfiguracionService configuracionService;

    @Test
    void shouldRejectUnsupportedConfigurationKey() {
        UUID idInstitucion = UUID.randomUUID();
        when(institucionRepository.findById(idInstitucion))
                .thenReturn(Optional.of(Institucion.builder().id(idInstitucion).tipoInstitucion("FISCAL").build()));

        ConfiguracionInstitucionRequest request = new ConfiguracionInstitucionRequest();
        request.setClave("CLAVE_DESCONOCIDA");
        request.setValor("valor");

        assertThrows(IllegalArgumentException.class, () -> configuracionService.guardar(idInstitucion, request));
    }

    @Test
    void shouldResolveFiscalDefaults() {
        UUID idInstitucion = UUID.randomUUID();
        when(institucionRepository.findById(idInstitucion))
                .thenReturn(Optional.of(Institucion.builder().id(idInstitucion).tipoInstitucion("FISCAL").build()));
        when(configuracionRepository.findAllByIdInstitucion(idInstitucion)).thenReturn(java.util.List.of());

        assertEquals("4", configuracionService.getText(idInstitucion, "CANTIDAD_PERIODOS"));
    }
}
