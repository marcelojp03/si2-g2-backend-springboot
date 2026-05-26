package com.uagrm.si2g2.auditoria.application;

import com.uagrm.si2g2.auditoria.domain.BitacoraAuditoria;
import com.uagrm.si2g2.auditoria.domain.BitacoraAuditoriaRepository;
import com.uagrm.si2g2.auditoria.dto.BitacoraAuditoriaFiltro;
import com.uagrm.si2g2.auditoria.dto.BitacoraAuditoriaResponse;
import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.tenant.TenantContext;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditoriaQueryService {

    private final BitacoraAuditoriaRepository repository;

    @Transactional(readOnly = true)
    public List<BitacoraAuditoriaResponse> listar(BitacoraAuditoriaFiltro filtro) {
        Specification<BitacoraAuditoria> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (!SecurityUtils.currentUserHasRole("SUPER_ADMIN")) {
                UUID idInstitucion = TenantContext.get() != null ? TenantContext.get() : SecurityUtils.requireCurrentInstitutionId();
                predicates.add(cb.equal(root.get("idInstitucion"), idInstitucion));
            }

            if (filtro.getModulo() != null && !filtro.getModulo().isBlank()) {
                predicates.add(cb.equal(root.get("nombreModulo"), filtro.getModulo()));
            }
            if (filtro.getTipoOperacion() != null && !filtro.getTipoOperacion().isBlank()) {
                predicates.add(cb.equal(root.get("tipoOperacion"), filtro.getTipoOperacion()));
            }
            if (filtro.getExito() != null) {
                predicates.add(cb.equal(root.get("exito"), filtro.getExito()));
            }
            if (filtro.getIdUsuario() != null) {
                predicates.add(cb.equal(root.get("idUsuario"), filtro.getIdUsuario()));
            }
            if (filtro.getFechaDesde() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fechaEvento"), filtro.getFechaDesde()));
            }
            if (filtro.getFechaHasta() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fechaEvento"), filtro.getFechaHasta()));
            }

            query.orderBy(cb.desc(root.get("fechaEvento")));
            return cb.and(predicates.toArray(Predicate[]::new));
        };

        return repository.findAll(specification).stream()
                .limit(200)
                .map(BitacoraAuditoriaResponse::from)
                .toList();
    }
}
