package com.uagrm.si2g2.tutor.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EstudianteTutorRepository extends JpaRepository<EstudianteTutor, UUID> {

    List<EstudianteTutor> findAllByIdInstitucionAndIdEstudiante(UUID idInstitucion, UUID idEstudiante);

    List<EstudianteTutor> findAllByIdInstitucionAndIdTutor(UUID idInstitucion, UUID idTutor);

    Optional<EstudianteTutor> findByIdInstitucionAndIdEstudianteAndIdTutor(UUID idInstitucion, UUID idEstudiante, UUID idTutor);

    boolean existsByIdInstitucionAndIdEstudianteAndIdTutor(UUID idInstitucion, UUID idEstudiante, UUID idTutor);

    boolean existsByIdInstitucionAndIdEstudianteAndEsPrincipalTrueAndEstado(UUID idInstitucion, UUID idEstudiante, String estado);

    @Modifying
    @Query("UPDATE EstudianteTutor et SET et.esPrincipal = false WHERE et.idInstitucion = :idInstitucion AND et.idEstudiante = :idEstudiante AND et.esPrincipal = true")
    void desmarcarPrincipalPorEstudiante(UUID idInstitucion, UUID idEstudiante);
}
