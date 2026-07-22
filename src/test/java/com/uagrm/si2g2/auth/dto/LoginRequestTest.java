package com.uagrm.si2g2.auth.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoginRequestTest {

    @Test
    void normalizesEmailWithoutChangingPassword() {
        LoginRequest request = new LoginRequest();
        request.setCorreo("  RobertoCervantes@GMAIL.COM  ");
        request.setContrasena(" Password With Spaces ");

        assertEquals("robertocervantes@gmail.com", request.getCorreo());
        assertEquals(" Password With Spaces ", request.getContrasena());
    }
}
