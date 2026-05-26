package com.uagrm.si2g2.saas.privilegio.dto;

import java.util.UUID;

public record PrivilegioUiResponse(
        UUID id,
        UUID idInstitucion,
        UUID idRol,
        String modulo,
        String entidad,
        String campo,
        String visibilidad,
        String edicion
) {}
