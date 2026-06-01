package com.uagrm.si2g2.common.email.api;

import com.uagrm.si2g2.common.dto.ApiResponse;
import com.uagrm.si2g2.common.email.EmailService;
import com.uagrm.si2g2.common.email.dto.EmailPruebaRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/saas/email")
@RequiredArgsConstructor
public class EmailTestController {

    private final EmailService emailService;

    @PostMapping("/prueba")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<Void> enviarCorreoPrueba(@Valid @RequestBody EmailPruebaRequest request) {
        emailService.enviarCorreoPrueba(request.getDestinatario(), request.getAsunto(), request.getMensaje());
        return ApiResponse.ok("Correo de prueba procesado", null);
    }
}
