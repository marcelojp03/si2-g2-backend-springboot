package com.uagrm.si2g2.alertas.application;

import com.uagrm.si2g2.alertas.domain.AlertaRiesgo;
import com.uagrm.si2g2.alertas.domain.AlertaRiesgoRepository;
import com.uagrm.si2g2.alertas.domain.RecomendacionIa;
import com.uagrm.si2g2.alertas.domain.RecomendacionIaRepository;
import com.uagrm.si2g2.alertas.dto.AlertaRiesgoResponse;
import com.uagrm.si2g2.alertas.dto.RecomendacionIaResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AlertaRiesgoService {

    private final AlertaRiesgoRepository repository;
    private final RecomendacionIaRepository recomendacionRepository;

    @Transactional(readOnly = true)
    public List<AlertaRiesgoResponse> listar(UUID idInstitucion, UUID idGestion, String nivel) {
        return repository.buscarConFiltros(idInstitucion, idGestion, nivel)
                .stream().map(AlertaRiesgoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AlertaRiesgoResponse obtener(UUID id, UUID idInstitucion) {
        return AlertaRiesgoResponse.from(
                repository.findByIdAndIdInstitucion(id, idInstitucion)
                        .orElseThrow(() -> new EntityNotFoundException("Alerta no encontrada: " + id)));
    }

    @Transactional(readOnly = true)
    public List<AlertaRiesgoResponse> listarPorEstudiante(UUID idEstudiante) {
        return repository.findByIdEstudianteAndActivaTrue(idEstudiante)
                .stream().map(AlertaRiesgoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<RecomendacionIaResponse> recomendaciones(UUID idAlerta) {
        return recomendacionRepository.findByIdAlertaRiesgoOrderByCreadoEnDesc(idAlerta)
                .stream().map(RecomendacionIaResponse::from).toList();
    }
}
