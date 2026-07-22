package com.uagrm.si2g2.config;

import com.uagrm.si2g2.security.JwtAuthFilter;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class SecurityConfigTest {

    @Test
    void allowsConfiguredLocalPreviewPatterns() {
        AppProperties properties = new AppProperties();
        properties.getCors().setAllowedOrigins(List.of("http://localhost:4200"));
        properties.getCors().setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
        SecurityConfig securityConfig = new SecurityConfig(
                mock(JwtAuthFilter.class), mock(AuthenticationProvider.class), properties);

        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) securityConfig.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/auth/login");
        CorsConfiguration cors = source.getCorsConfiguration(request);

        assertEquals("http://localhost:62248", cors.checkOrigin("http://localhost:62248"));
        assertEquals("http://127.0.0.1:5173", cors.checkOrigin("http://127.0.0.1:5173"));
        assertEquals("http://localhost:4200", cors.checkOrigin("http://localhost:4200"));
    }
}
