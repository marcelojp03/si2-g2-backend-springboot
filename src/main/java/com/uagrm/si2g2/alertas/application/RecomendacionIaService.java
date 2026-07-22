package com.uagrm.si2g2.alertas.application;

import com.uagrm.si2g2.alertas.domain.RecomendacionIa;
import com.uagrm.si2g2.alertas.domain.RecomendacionIaRepository;
import com.uagrm.si2g2.alertas.dto.RecomendacionIaResponse;
import com.uagrm.si2g2.alertas.domain.AlertaRiesgoRepository;
import com.uagrm.si2g2.common.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecomendacionIaService {

    private final RecomendacionIaRepository repository;
    private final AlertaRiesgoRepository alertaRepository;

    @Transactional(readOnly = true)
    public List<RecomendacionIaResponse> listarPorAlerta(UUID idAlerta) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        alertaRepository.findByIdAndIdInstitucion(idAlerta, idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Alerta no encontrada: " + idAlerta));
        return repository.findByAlertaAndInstitucion(idAlerta, idInstitucion)
                .stream().map(RecomendacionIaResponse::from).toList();
    }
}
