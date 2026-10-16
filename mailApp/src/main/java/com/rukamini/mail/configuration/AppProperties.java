package com.rukamini.mail.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Typed {@code app.*} configuration. Secret values are resolved from Azure Key Vault when it is
 * enabled (see {@code application.properties}) and fall back to environment variables otherwise.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** Logical environment name (for example LOCAL, TRAINING, PROD). */
    @NotBlank private String environment;

    /** Default database schema used by Hibernate. */
    @NotBlank private String defaultSchema;

    @Valid private final Datasource datasource = new Datasource();

    @Valid private final Mail mail = new Mail();

    @Valid private final Urls urls = new Urls();

    @Valid private final OAuth2 oauth2 = new OAuth2();

    @Getter
    @Setter
    public static class Datasource {
        @Valid private final Cwf cwf = new Cwf();
    }

    /** PostgreSQL {@code cwf} datasource and its Hikari pool. */
    @Getter
    @Setter
    public static class Cwf {
        @NotBlank private String url;

        @NotBlank private String driverClassName = "org.postgresql.Driver";

        private String username;

        private String password;

        @Valid private final Pool pool = new Pool();
    }

    @Getter
    @Setter
    public static class Pool {
        @Min(0)
        private int minIdle = 1;

        @Min(1)
        private int maxSize = 50;

        @Min(250)
        private long connectionTimeoutMs = 180_000;

        @Min(250)
        private long validationTimeoutMs = 30_000;

        @Min(10_000)
        private long idleTimeoutMs = 1_200_000;

        @Min(30_000)
        private long maxLifetimeMs = 1_800_000;
    }

    @Getter
    @Setter
    public static class Mail {
        @NotBlank private String host;

        @Min(1)
        private int port = 25;

        @NotBlank private String senderAddress;

        private String techUser;

        private String techPass;

        @Valid private final Smtp smtp = new Smtp();
    }

    @Getter
    @Setter
    public static class Smtp {
        private boolean auth = true;
        private boolean starttls = true;
        private String sslTrust;
    }

    @Getter
    @Setter
    public static class Urls {
        @NotBlank private String pom;
        @NotBlank private String deal;
        @NotBlank private String dealApp;
        @NotBlank private String renewalOverview;
    }

    @Getter
    @Setter
    public static class OAuth2 {
        private String swisscomClient;
        private String swisscomSecret;
        @NotBlank private String uwbScope;
        @NotBlank private String pomScope;
        @NotBlank private String graphScope;
    }
}
