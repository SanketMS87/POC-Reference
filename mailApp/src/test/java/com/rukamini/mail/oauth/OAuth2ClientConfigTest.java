package com.rukamini.mail.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("OAuth2ClientConfig")
class OAuth2ClientConfigTest {

    private static final String TOKEN_URI =
            "https://login.microsoftonline.com/45597f60-6e37-4be7-acfb-4c9e23b261ea/oauth2/v2.0/token";
    private static final String CLIENT_ID = "test-client-id";
    private static final String CLIENT_SECRET = "test-client-secret";

    /** UWB-default scope — mirrors the app.oauth2.uwb-scope property. */
    private static final String UWB_SCOPE = "api://cf62061e-1d78-4a1a-8ef3-114e09464d68/.default";

    /** POM-default scope — mirrors the app.oauth2.pom-scope property. */
    private static final String POM_SCOPE = "api://pom-train.swissre.com/.default";

    private OAuth2ClientConfig config;

    @BeforeEach
    void setUp() {
        config = new OAuth2ClientConfig();
        ReflectionTestUtils.setField(config, "tokenUri", TOKEN_URI);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private ClientRegistration buildUwb() {
        return config.uwbClientRegistration(CLIENT_ID, CLIENT_SECRET, UWB_SCOPE);
    }

    private ClientRegistration buildPom() {
        return config.pomClientRegistration(CLIENT_ID, CLIENT_SECRET, POM_SCOPE);
    }

    private ClientRegistrationRepository buildRepo() {
        return config.clientRegistrationRepository(List.of(buildUwb(), buildPom()));
    }

    // ── uwbClientRegistration ─────────────────────────────────────────────────

    @Test
    @DisplayName("uwbClientRegistration returns registration with id 'uwb-default'")
    void uwbClientRegistration_registrationIdIsUwbDefault() {
        assertThat(buildUwb().getRegistrationId()).isEqualTo(OAuth2ClientConfig.UWB_DEFAULT);
    }

    @Test
    @DisplayName("uwbClientRegistration sets CLIENT_CREDENTIALS grant type")
    void uwbClientRegistration_grantTypeIsClientCredentials() {
        assertThat(buildUwb().getAuthorizationGrantType())
                .isEqualTo(AuthorizationGrantType.CLIENT_CREDENTIALS);
    }

    @Test
    @DisplayName("uwbClientRegistration wires clientId, clientSecret, scope and tokenUri")
    void uwbClientRegistration_credentialsAndEndpointSet() {
        ClientRegistration reg = buildUwb();
        assertThat(reg.getClientId()).isEqualTo(CLIENT_ID);
        assertThat(reg.getClientSecret()).isEqualTo(CLIENT_SECRET);
        assertThat(reg.getScopes()).containsOnly(UWB_SCOPE);
        assertThat(reg.getProviderDetails().getTokenUri()).isEqualTo(TOKEN_URI);
    }

    @Test
    @DisplayName("uwbClientRegistration scope is the UWBench App Registration")
    void uwbClientRegistration_scopeIsUwbAppRegistration() {
        assertThat(buildUwb().getScopes())
                .containsOnly("api://cf62061e-1d78-4a1a-8ef3-114e09464d68/.default");
    }

    // ── pomClientRegistration ─────────────────────────────────────────────────

    @Test
    @DisplayName("pomClientRegistration returns registration with id 'pom-default'")
    void pomClientRegistration_registrationIdIsPomDefault() {
        assertThat(buildPom().getRegistrationId()).isEqualTo(OAuth2ClientConfig.POM_DEFAULT);
    }

    @Test
    @DisplayName("pomClientRegistration sets CLIENT_CREDENTIALS grant type")
    void pomClientRegistration_grantTypeIsClientCredentials() {
        assertThat(buildPom().getAuthorizationGrantType())
                .isEqualTo(AuthorizationGrantType.CLIENT_CREDENTIALS);
    }

    @Test
    @DisplayName("pom-default scope is the POM resource audience, independent of uwb-default scope")
    void pomClientRegistration_scopeIsIndependentFromUwb() {
        assertThat(buildPom().getScopes()).containsOnly(POM_SCOPE);
        assertThat(buildPom().getScopes()).doesNotContain(UWB_SCOPE);
    }

    @Test
    @DisplayName("pom-default shares the same clientId and clientSecret as uwb-default")
    void pomClientRegistration_sharesCredentialsWithUwb() {
        assertThat(buildPom().getClientId()).isEqualTo(buildUwb().getClientId());
        assertThat(buildPom().getClientSecret()).isEqualTo(buildUwb().getClientSecret());
    }

    // ── clientRegistrationRepository ─────────────────────────────────────────

    @Test
    @DisplayName("clientRegistrationRepository builds InMemoryClientRegistrationRepository")
    void clientRegistrationRepository_returnsInMemoryRepository() {
        assertThat(buildRepo()).isInstanceOf(InMemoryClientRegistrationRepository.class);
    }

    @Test
    @DisplayName("clientRegistrationRepository contains both registrations")
    void clientRegistrationRepository_containsBothRegistrations() {
        ClientRegistrationRepository repo = buildRepo();
        assertThat(repo.findByRegistrationId(OAuth2ClientConfig.UWB_DEFAULT)).isNotNull();
        assertThat(repo.findByRegistrationId(OAuth2ClientConfig.POM_DEFAULT)).isNotNull();
    }

    @Test
    @DisplayName("clientRegistrationRepository never needs changing — list is collected by Spring")
    void clientRegistrationRepository_acceptsArbitraryRegistrationList() {
        ClientRegistration future =
                ClientRegistration.withRegistrationId("future-default")
                        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                        .tokenUri(TOKEN_URI)
                        .clientId(CLIENT_ID)
                        .clientSecret(CLIENT_SECRET)
                        .scope("api://future-app/.default")
                        .build();

        ClientRegistrationRepository repo =
                config.clientRegistrationRepository(List.of(buildUwb(), buildPom(), future));

        assertThat(repo.findByRegistrationId("future-default")).isNotNull();
        assertThat(repo.findByRegistrationId(OAuth2ClientConfig.UWB_DEFAULT)).isNotNull();
        assertThat(repo.findByRegistrationId(OAuth2ClientConfig.POM_DEFAULT)).isNotNull();
    }

    // ── authorizedClientService ───────────────────────────────────────────────

    @Test
    @DisplayName("authorizedClientService returns InMemoryOAuth2AuthorizedClientService")
    void authorizedClientService_returnsInMemoryService() {
        assertThat(config.authorizedClientService(buildRepo()))
                .isInstanceOf(InMemoryOAuth2AuthorizedClientService.class);
    }

    // ── authorizedClientManager ───────────────────────────────────────────────

    @Test
    @DisplayName(
            "authorizedClientManager returns"
                    + " AuthorizedClientServiceOAuth2AuthorizedClientManager")
    void authorizedClientManager_returnsCorrectManagerType() {
        ClientRegistrationRepository repo = buildRepo();
        OAuth2AuthorizedClientService service = config.authorizedClientService(repo);
        OAuth2AuthorizedClientManager manager = config.authorizedClientManager(repo, service);
        assertThat(manager)
                .isInstanceOf(AuthorizedClientServiceOAuth2AuthorizedClientManager.class);
    }

    // ── constants ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("UWB_DEFAULT constant value is 'uwb-default'")
    void uwbDefaultConstant_hasExpectedValue() {
        assertThat(OAuth2ClientConfig.UWB_DEFAULT).isEqualTo("uwb-default");
    }

    @Test
    @DisplayName("POM_DEFAULT constant value is 'pom-default'")
    void pomDefaultConstant_hasExpectedValue() {
        assertThat(OAuth2ClientConfig.POM_DEFAULT).isEqualTo("pom-default");
    }
}
