package com.uagrm.si2g2.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    @Valid
    private final Jwt jwt = new Jwt();

    @Valid
    private final SuperAdmin superAdmin = new SuperAdmin();

    @Valid
    private final Cors cors = new Cors();

    @Valid
    private final Aws aws = new Aws();

    @Valid
    private final Security security = new Security();

    @Getter
    @Setter
    public static class Jwt {

        @NotBlank
        private String secret;

        @Min(1)
        private long expiration;
    }

    @Getter
    @Setter
    public static class SuperAdmin {

        @NotBlank
        private String correo;

        @NotBlank
        private String contrasena;

        @NotBlank
        private String nombres = "Super";

        @NotBlank
        private String apellidos = "Admin";
    }

    @Getter
    @Setter
    public static class Cors {

        @NotEmpty
        private List<String> allowedOrigins = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class Aws {

        @Valid
        private final S3 s3 = new S3();
    }

    @Getter
    @Setter
    public static class S3 {

        @NotBlank
        private String bucket;

        @NotBlank
        private String region;

        @Min(1)
        private int presignedUrlExpirationMinutes = 60;
    }

    @Getter
    @Setter
    public static class Security {

        @Valid
        private final AccountRecovery accountRecovery = new AccountRecovery();

        @Valid
        private final Audit audit = new Audit();
    }

    @Getter
    @Setter
    public static class AccountRecovery {

        @Min(1)
        private int expirationMinutes = 15;

        @Min(1)
        private int maxAttempts = 5;

        private boolean exposeDebugData = false;
    }

    @Getter
    @Setter
    public static class Audit {

        @NotBlank
        private String hashSecret = "local-audit-secret-change-me";
    }

    /** URL base del frontend (para construir links en correos). */
    private String frontendUrl = "http://localhost:4200";
}
