package com.uagrm.si2g2.usuario.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedUsuarioResponse {
    private List<UsuarioResponse> usuarios;
    private long total;
    private int pagina;
    private int totalPaginas;
}
