package com.uagrm.si2g2.alertas.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.uagrm.si2g2.curso.domain.ParaleloRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiesgoAcademicoTenantProcessor {
    private final ParaleloRepository paraleloRepository;
    private final RiesgoAcademicoParaleloProcessor paraleloProcessor;

    public void procesar(UUID idInstitucion, UUID idGestion) {
        paraleloRepository.findAllByIdInstitucionAndIdGestionAcademica(idInstitucion, idGestion).stream()
                .filter(paralelo -> "ACTIVO".equals(paralelo.getEstado()))
                .forEach(paralelo -> {
                    try {
                        paraleloProcessor.procesar(idInstitucion, paralelo.getId(), idGestion);
                    } catch (Exception exception) {
                        log.error("Error en analisis de riesgo para institucion {}, gestion {} y paralelo {}",
                                idInstitucion, idGestion, paralelo.getId(), exception);
                    }
                });
    }
}
