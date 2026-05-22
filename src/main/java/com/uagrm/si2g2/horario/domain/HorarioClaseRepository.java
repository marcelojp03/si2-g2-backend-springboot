package com.uagrm.si2g2.horario.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface HorarioClaseRepository extends JpaRepository<HorarioClase, UUID> {

    List<HorarioClase> findByIdInstitucionAndEstado(UUID idInstitucion, String estado);

    List<HorarioClase> findByIdAsignacionDocenteAndEstado(UUID idAsignacionDocente, String estado);

    List<HorarioClase> findByIdAulaAndEstado(UUID idAula, String estado);

    @Query("""
            select h
            from HorarioClase h
            where h.idInstitucion = :idInstitucion
              and h.idAula = :idAula
              and h.diaSemana = :diaSemana
              and h.estado = 'ACTIVO'
              and h.horaInicio < :horaFin
              and h.horaFin > :horaInicio
              and (:idExcluir is null or h.id <> :idExcluir)
            """)
    List<HorarioClase> buscarConflictoAula(@Param("idInstitucion") UUID idInstitucion,
                                           @Param("idAula") UUID idAula,
                                           @Param("diaSemana") String diaSemana,
                                           @Param("horaInicio") LocalTime horaInicio,
                                           @Param("horaFin") LocalTime horaFin,
                                           @Param("idExcluir") UUID idExcluir);

    @Query("""
            select h
            from HorarioClase h
            join AsignacionDocente ad on ad.id = h.idAsignacionDocente
            where h.idInstitucion = :idInstitucion
              and ad.idDocente = :idDocente
              and h.diaSemana = :diaSemana
              and h.estado = 'ACTIVO'
              and h.horaInicio < :horaFin
              and h.horaFin > :horaInicio
              and (:idExcluir is null or h.id <> :idExcluir)
            """)
    List<HorarioClase> buscarConflictoDocente(@Param("idInstitucion") UUID idInstitucion,
                                              @Param("idDocente") UUID idDocente,
                                              @Param("diaSemana") String diaSemana,
                                              @Param("horaInicio") LocalTime horaInicio,
                                              @Param("horaFin") LocalTime horaFin,
                                              @Param("idExcluir") UUID idExcluir);

    @Query("""
            select h
            from HorarioClase h
            join AsignacionDocente ad on ad.id = h.idAsignacionDocente
            where h.idInstitucion = :idInstitucion
              and ad.idParalelo = :idParalelo
              and h.diaSemana = :diaSemana
              and h.estado = 'ACTIVO'
              and h.horaInicio < :horaFin
              and h.horaFin > :horaInicio
              and (:idExcluir is null or h.id <> :idExcluir)
            """)
    List<HorarioClase> buscarConflictoParalelo(@Param("idInstitucion") UUID idInstitucion,
                                               @Param("idParalelo") UUID idParalelo,
                                               @Param("diaSemana") String diaSemana,
                                               @Param("horaInicio") LocalTime horaInicio,
                                               @Param("horaFin") LocalTime horaFin,
                                               @Param("idExcluir") UUID idExcluir);
}
