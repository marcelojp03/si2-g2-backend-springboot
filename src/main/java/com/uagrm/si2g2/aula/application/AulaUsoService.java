package com.uagrm.si2g2.aula.application;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AulaUsoService {

    public boolean tieneHorariosActivosEnGestionActual(UUID idInstitucion, UUID idAula) {
        return false;
    }
}
