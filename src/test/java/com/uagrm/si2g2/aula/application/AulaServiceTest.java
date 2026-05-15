package com.uagrm.si2g2.aula.application;

import com.uagrm.si2g2.auditoria.application.AuditoriaService;
import com.uagrm.si2g2.aula.domain.Aula;
import com.uagrm.si2g2.aula.domain.AulaRepository;
import com.uagrm.si2g2.aula.dto.AulaRequest;
import com.uagrm.si2g2.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AulaServiceTest {

    @Mock
    private AulaRepository repository;

    @Mock
    private AulaUsoService aulaUsoService;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private AulaService service;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.set(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldRejectDuplicatedCodeInsideInstitution() {
        AulaRequest request = request();
        when(repository.existsByIdInstitucionAndCodigo(tenantId, "AULA-101")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.crear(request));
        verify(repository, never()).save(any());
    }

    @Test
    void shouldUseTenantAndFiltersWhenListing() {
        when(repository.findAllByIdInstitucionOrderByEstadoAscNombreAsc(tenantId))
                .thenReturn(List.of(aula(UUID.randomUUID())));

        var result = service.listar("ACTIVO", 20, 40, "Proyector", "Bloque A");

        assertEquals(1, result.size());
        verify(repository).findAllByIdInstitucionOrderByEstadoAscNombreAsc(tenantId);
    }

    @Test
    void shouldRejectInvalidCapacityRange() {
        assertThrows(IllegalArgumentException.class, () -> service.listar(null, 50, 20, null, null));
        verify(repository, never()).findAllByIdInstitucionOrderByEstadoAscNombreAsc(any());
    }

    @Test
    void shouldBlockDeletionWhenAulaHasActiveSchedules() {
        UUID idAula = UUID.randomUUID();
        when(repository.findByIdAndIdInstitucion(idAula, tenantId)).thenReturn(Optional.of(aula(idAula)));
        when(aulaUsoService.tieneHorariosActivosEnGestionActual(tenantId, idAula)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.eliminar(idAula));
        verify(repository, never()).save(any());
    }

    @Test
    void shouldSoftDeleteWhenAulaHasNoActiveSchedules() {
        UUID idAula = UUID.randomUUID();
        Aula aula = aula(idAula);
        when(repository.findByIdAndIdInstitucion(idAula, tenantId)).thenReturn(Optional.of(aula));
        when(aulaUsoService.tieneHorariosActivosEnGestionActual(tenantId, idAula)).thenReturn(false);

        service.eliminar(idAula);

        assertEquals("INACTIVO", aula.getEstado());
        verify(repository).save(aula);
    }

    private AulaRequest request() {
        AulaRequest request = new AulaRequest();
        request.setCodigo("AULA-101");
        request.setNombre("Aula 101");
        request.setCapacidad(30);
        request.setUbicacion("Bloque A");
        request.setRecursos(List.of("Pizarra", "Proyector"));
        return request;
    }

    private Aula aula(UUID id) {
        return Aula.builder()
                .id(id)
                .idInstitucion(tenantId)
                .codigo("AULA-101")
                .nombre("Aula 101")
                .capacidad(30)
                .ubicacion("Bloque A")
                .recursos("Pizarra|Proyector")
                .estado("ACTIVO")
                .build();
    }
}
