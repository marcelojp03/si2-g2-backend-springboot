package com.uagrm.si2g2.pagos.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PagoRepository extends JpaRepository<Pago, UUID> {

    List<Pago> findByIdCuotaOrderByCreadoEnDesc(UUID idCuota);

    List<Pago> findByIdUsuarioPagaOrderByCreadoEnDesc(UUID idUsuario);

    List<Pago> findByIdInstitucionAndEstado(UUID idInstitucion, String estado);
}
