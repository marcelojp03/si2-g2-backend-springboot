package com.uagrm.si2g2.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final int codigo;
    private final String mensaje;
    private final T data;

    private ApiResponse(int codigo, String mensaje, T data) {
        this.codigo = codigo;
        this.mensaje = mensaje;
        this.data = data;
    }

    public static <T> ApiResponse<T> ok(String mensaje, T data) {
        return new ApiResponse<>(HttpStatus.OK.value(), mensaje, data);
    }

    public static <T> ApiResponse<T> created(String mensaje, T data) {
        return new ApiResponse<>(HttpStatus.CREATED.value(), mensaje, data);
    }

    public static <T> ApiResponse<T> error(int codigo, String mensaje) {
        return new ApiResponse<>(codigo, mensaje, null);
    }
}
