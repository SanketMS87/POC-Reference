package com.rukamini.mail.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Typed {@code app.security.*} configuration for inbound bearer-token (JWT) validation. */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    /**
     * When true every valid token receives all known roles; when false only the token's roles
     * claim is used.
     */
    private boolean grantAllRolesToAuthenticated = false;

    @Valid private final Jwt jwt = new Jwt();

    @Getter
    @Setter
    public static class Jwt {
        @NotBlank private String jwkSetUri;

        @NotBlank private String issuer;

        @NotEmpty private List<String> audiences = new ArrayList<>();

        @NotBlank private String userNameClaim = "sub";

        @NotBlank private String rolesClaim = "roles";
    }
}
