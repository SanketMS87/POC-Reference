package com.rukamini.mail.oauth;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ClientCredentialsOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

/**
 * Wires the Spring Security OAuth2 Client infrastructure for outbound service-to-service calls.
 *
 * <p>Operates in <em>service / programmatic mode only</em> — no servlet filters and no login
 * redirects. {@code OAuth2ClientAutoConfiguration} is excluded in
 * {@link com.rukamini.mail.configuration.RestApplication} so that the inbound resource-server
 * filter chain is completely unaffected.
 *
 * <p>Credentials are sourced from the property-backed beans defined in
 * {@link com.rukamini.mail.configuration.AppConfig} ({@code app.oauth2.*}; the client id and
 * secret come from environment variables).
 *
 * <h2>Scope / audience isolation</h2>
 *
 * <p>In Entra's {@code client_credentials} grant, each token is issued for exactly one resource
 * application (the App ID URI that appears before {@code /.default} in the scope). Tokens for
 * different resource applications therefore require separate {@link ClientRegistration} entries —
 * each with the same {@code clientId}/{@code clientSecret} but a different {@code scope}.
 * {@link InMemoryOAuth2AuthorizedClientService} caches them independently keyed on
 * {@code (registrationId, principal)}.
 *
 * <h2>How to add a new registration</h2>
 *
 * <ol>
 *   <li>Declare a new constant below.
 *   <li>Add a {@code @Bean}-annotated {@link ClientRegistration} method in this class, injecting
 *       only the credentials and scope beans it needs.
 *   <li>Add the corresponding {@code app.oauth2.*} properties to the relevant
 *       {@code application-<profile>.properties} files.
 * </ol>
 *
 * <p>No changes to {@link #clientRegistrationRepository} are ever needed — Spring automatically
 * collects every {@link ClientRegistration} bean in the context into the repository.
 *
 * <p>Current registrations:
 *
 * <ul>
 *   <li>{@link #UWB_DEFAULT} — all UWBench-internal services
 *   <li>{@link #POM_DEFAULT} — Policy Object Manager; same credentials as UWB_DEFAULT but
 *       different resource audience requiring an independently cached token
 * </ul>
 */
@Slf4j
@Configuration
public class OAuth2ClientConfig {

    // ── Registration ID constants ─────────────────────────────────────────────

    private static final String LOG_REGISTERING =
            "OAuth2ClientConfig: registering '{}' \u2014 clientId='{}' tokenUri='{}' scope='{}'";

    /**
     * Registration ID for all UWBench-internal service calls. Scope: {@code
     * api://cf62061e-1d78-4a1a-8ef3-114e09464d68/.default} (NP) /
     * {@code api://76457445-4c4c-4090-a269-c0f60e6f4626/.default} (prod).
     */
    public static final String UWB_DEFAULT = "uwb-default";

    /**
     * Registration ID for POM (Policy Object Manager) service calls. Scope: {@code
     * api://pom-train.swissre.com/.default} (NP) / {@code api://pom.swissre.com/.default} (prod).
     * Uses the same {@code swisscomOAuthClient} / {@code swisscomOAuthSecret} credentials as
     * {@link #UWB_DEFAULT} but targets a different resource application audience, so it requires
     * its own independently cached token.
     */
    public static final String POM_DEFAULT = "pom-default";

    /**
     * Registration ID for Microsoft Graph service calls.
     * Scope: {@code https://graph.microsoft.com/.default}.
     *
     * <p>Reuses the same {@code swisscomOAuthClient} / {@code swisscomOAuthSecret} credentials
     * (which already hold the required {@code User.Read.All} application permission in Entra)
     * but targets the Graph resource audience, so it requires its own independently cached token.
     */
    public static final String GRAPH_DEFAULT = "graph-default";

    // ── Configuration ─────────────────────────────────────────────────────────

    @Value("${token-issuer}")
    private String tokenUri;

    // ── Individual ClientRegistration beans ───────────────────────────────────

    /**
     * {@link ClientRegistration} for {@link #UWB_DEFAULT}.
     *
     * <p>To add a new registration in the future: copy this pattern, declare a new constant above,
     * inject the credential/scope beans you need, and return {@link #buildRegistration}. No other
     * method in this class needs to change.
     */
    @Bean
    public ClientRegistration uwbClientRegistration(
            @Qualifier("swisscomOAuthClient") String clientId,
            @Qualifier("swisscomOAuthSecret") String clientSecret,
            @Qualifier("uwbDefaultOauth2Scope") String scope) {
        log.info(LOG_REGISTERING, UWB_DEFAULT, clientId, tokenUri, scope);
        return buildRegistration(UWB_DEFAULT, clientId, clientSecret, scope);
    }

    /**
     * {@link ClientRegistration} for {@link #POM_DEFAULT}. Same credentials as {@link
     * #UWB_DEFAULT}; only the scope (resource audience) differs.
     */
    @Bean
    public ClientRegistration pomClientRegistration(
            @Qualifier("swisscomOAuthClient") String clientId,
            @Qualifier("swisscomOAuthSecret") String clientSecret,
            @Qualifier("pomDefaultOauth2Scope") String scope) {
        log.info(LOG_REGISTERING, POM_DEFAULT, clientId, tokenUri, scope);
        return buildRegistration(POM_DEFAULT, clientId, clientSecret, scope);
    }

    /**
     * {@link ClientRegistration} for {@link #GRAPH_DEFAULT}.
     *
     * <p>Uses the same {@code swisscomOAuthClient} / {@code swisscomOAuthSecret}
     * credentials as the other registrations; only the scope differs
     * ({@code https://graph.microsoft.com/.default}).
     */
    @Bean
    public ClientRegistration graphClientRegistration(
            @Qualifier("swisscomOAuthClient") String clientId,
            @Qualifier("swisscomOAuthSecret") String clientSecret,
            @Qualifier("graphOauth2Scope") String scope) {
        log.info(LOG_REGISTERING, GRAPH_DEFAULT, clientId, tokenUri, scope);
        return buildRegistration(GRAPH_DEFAULT, clientId, clientSecret, scope);
    }

    // ── Repository / manager / cache ─────────────────────────────────────────

    /**
     * Collects every {@link ClientRegistration} bean declared in the context into a single
     * {@link InMemoryClientRegistrationRepository}.
     *
     * <p><strong>This method never needs to change.</strong> Spring automatically injects all
     * {@link ClientRegistration} beans — adding a new registration is simply adding a new
     * {@code @Bean} method above.
     */
    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(
            List<ClientRegistration> registrations) {
        registrations.forEach(
                r ->
                        log.info(
                                "OAuth2ClientConfig: loading registration '{}' into repository",
                                r.getRegistrationId()));
        return new InMemoryClientRegistrationRepository(registrations);
    }

    /**
     * In-memory token cache. Tokens are stored keyed on {@code (registrationId, principalName)}
     * and reused until their {@code expires_in} lapses (~3600 s for Entra), at which point the
     * provider fetches a fresh one automatically.
     */
    @Bean
    public OAuth2AuthorizedClientService authorizedClientService(
            ClientRegistrationRepository clientRegistrationRepository) {
        return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
    }

    /**
     * The single programmatic entry point for token acquisition. Uses {@link
     * ClientCredentialsOAuth2AuthorizedClientProvider}, which performs the token-endpoint POST via
     * a plain {@link org.springframework.web.client.RestTemplate} that respects the JVM proxy
     * system properties set in {@code jvm.options}.
     */
    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService) {

        ClientCredentialsOAuth2AuthorizedClientProvider provider =
                new ClientCredentialsOAuth2AuthorizedClientProvider();

        AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                        clientRegistrationRepository, authorizedClientService);
        manager.setAuthorizedClientProvider(provider);

        log.info(
                "OAuth2ClientConfig: AuthorizedClientServiceOAuth2AuthorizedClientManager"
                        + " initialised with ClientCredentialsOAuth2AuthorizedClientProvider"
                        + " (in-memory token cache active)");

        return manager;
    }

    // ── private helpers ───────────────────────────────────────────────────────

    /**
     * Builds a {@code client_credentials} {@link ClientRegistration} for the given parameters.
     */
    private ClientRegistration buildRegistration(
            String registrationId, String clientId, String clientSecret, String scope) {
        return ClientRegistration.withRegistrationId(registrationId)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenUri(tokenUri)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .scope(scope)
                .build();
    }
}
