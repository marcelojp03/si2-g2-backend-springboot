package com.uagrm.si2g2.saas.privilegio.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PrivilegioUiRepository extends JpaRepository<PrivilegioUi, UUID> {

    List<PrivilegioUi> findAllByIdInstitucionAndIdRol(UUID idInstitucion, UUID idRol);

    List<PrivilegioUi> findAllByIdInstitucionAndIdRolAndModulo(UUID idInstitucion, UUID idRol, String modulo);

    List<PrivilegioUi> findAllByIdInstitucion(UUID idInstitucion);

    void deleteAllByIdInstitucionAndIdRol(UUID idInstitucion, UUID idRol);
}
