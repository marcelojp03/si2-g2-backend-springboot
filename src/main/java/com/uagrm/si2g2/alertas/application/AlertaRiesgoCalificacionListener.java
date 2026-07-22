package com.uagrm.si2g2.alertas.application;

import com.uagrm.si2g2.alertas.domain.AlertaRiesgoRepository;
import com.uagrm.si2g2.calificacion.application.CalificacionesActualizadasEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AlertaRiesgoCalificacionListener {

    private final AlertaRiesgoRepository alertaRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marcarAlertasDesactualizadas(CalificacionesActualizadasEvent event) {
        event.idsEstudiante().forEach(idEstudiante -> alertaRepository
                .findByIdInstitucionAndIdEstudianteAndIdGestionAcademicaAndActivaTrue(
                        event.idInstitucion(), idEstudiante, event.idGestion())
                .ifPresent(alerta -> {
                    alerta.setDatosVigentes(false);
                    alertaRepository.save(alerta);
                }));
    }
}
