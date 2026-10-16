package com.rukamini.mail.oauth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;

/**
 * Minimal standalone Spring context for the OAuth2 live integration smoke-tests.
 *
 * <p>Replaces the property-backed beans that {@link OAuth2ClientConfig} normally receives from
 * {@code AppConfig} (which requires the full application configuration) with values sourced from
 * {@code oauth2-it.properties} on the test classpath. Only the credential beans and the
 * token-issuer property are declared — no database dependency.
 *
 * <p>Uses {@code @Configuration} (not {@code @TestConfiguration}) so that
 * {@code @SpringJUnitConfig(classes = ...)} can treat this as the root context rather than an
 * additive overlay.
 */
@Configuration
@Import(OAuth2ClientConfig.class)
public class Oauth2TokenServiceIntegrationTestConfig {

    @Value("${test.oauth2.clientId}")
    private String clientId;

    @Value("${test.oauth2.clientSecret}")
    private String clientSecret;

    @Value("${test.oauth2.scope}")
    private String scope;

    /** Satisfies the swisscomOAuthClient qualifier consumed by OAuth2ClientConfig. */
    @Bean("swisscomOAuthClient")
    public String swisscomOAuthClient() {
        return clientId;
    }

    /** Satisfies the swisscomOAuthSecret qualifier consumed by OAuth2ClientConfig. */
    @Bean("swisscomOAuthSecret")
    public String swisscomOAuthSecret() {
        return clientSecret;
    }

    /** Satisfies the uwbDefaultOauth2Scope qualifier consumed by OAuth2ClientConfig. */
    @Bean("uwbDefaultOauth2Scope")
    public String uwbDefaultOauth2Scope() {
        return scope;
    }

    /**
     * Satisfies the pomDefaultOauth2Scope qualifier consumed by OAuth2ClientConfig. For the
     * integration smoke-test we point POM at the same scope as UWB (same Entra app), since the
     * test validates token acquisition mechanics, not audience routing.
     */
    @Bean("pomDefaultOauth2Scope")
    public String pomDefaultOauth2Scope() {
        return scope;
    }

    /**
     * Registers Oauth2TokenService so it can be autowired into the test. In production it is
     * picked up via @Component and a full @ComponentScan; here we wire it explicitly to keep
     * the context slice minimal.
     */
    @Bean
    public Oauth2TokenService oauth2TokenService(OAuth2AuthorizedClientManager manager) {
        return new Oauth2TokenService(manager);
    }
}
