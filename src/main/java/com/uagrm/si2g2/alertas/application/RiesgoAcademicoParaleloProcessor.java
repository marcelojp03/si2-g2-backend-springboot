package com.uagrm.si2g2.alertas.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RiesgoAcademicoParaleloProcessor {
    private final RiesgoAcademicoService riesgoAcademicoService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void procesar(UUID idInstitucion, UUID idParalelo, UUID idGestion) {
        riesgoAcademicoService.procesarParalelo(idInstitucion, idParalelo, idGestion);
    }
}
