package com.uagrm.si2g2.asignacion.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AsignacionDocenteRepository extends JpaRepository<AsignacionDocente, UUID> {

    List<AsignacionDocente> findAllByIdInstitucion(UUID idInstitucion);

    List<AsignacionDocente> findAllByIdInstitucionAndIdParalelo(UUID idInstitucion, UUID idParalelo);

    List<AsignacionDocente> findAllByIdInstitucionAndIdGestion(UUID idInstitucion, UUID idGestion);

    List<AsignacionDocente> findAllByIdInstitucionAndIdDocente(UUID idInstitucion, UUID idDocente);

    Optional<AsignacionDocente> findByIdAndIdInstitucion(UUID id, UUID idInstitucion);

    boolean existsByIdInstitucionAndIdDocenteAndIdMateriaAndIdParaleloAndIdGestion(
            UUID idInstitucion, UUID idDocente, UUID idMateria, UUID idParalelo, UUID idGestion);
}
