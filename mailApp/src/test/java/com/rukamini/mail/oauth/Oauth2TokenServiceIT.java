package com.rukamini.mail.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Live integration smoke-test for the Spring Security OAuth2 Client infrastructure.
 *
 * <p><strong>What this tests:</strong> the full bean chain
 * {@code OAuth2ClientConfig → ClientRegistrationRepository → OAuth2AuthorizedClientManager →
 * Oauth2TokenService} actually performs an HTTPS POST to the Entra token endpoint and returns a
 * valid Bearer token. No database — only an outbound HTTPS call to Microsoft Entra.
 *
 * <p><strong>Running from the IDE:</strong> fill in {@code oauth2-it.properties} with real
 * credentials and right-click → Run.
 *
 * <p><strong>Running from Maven:</strong>
 *
 * <pre>
 * mvn test -Poauth2-it \
 *   -Dtest.oauth2.clientId=&lt;id&gt; \
 *   -Dtest.oauth2.clientSecret=&lt;secret&gt; \
 *   -Dtest.oauth2.scope=&lt;scope&gt;
 * </pre>
 *
 * <p>The {@code oauth2-it.properties} file is git-ignored — never commit real credentials.
 */
@Tag("integration")
@SpringJUnitConfig(classes = Oauth2TokenServiceIntegrationTestConfig.class)
@TestPropertySource(
        locations = "classpath:oauth2-it.properties",
        // Bind test.oauth2.tokenUri → token-issuer so that
        // @Value("${token-issuer}") in OAuth2ClientConfig resolves correctly.
        properties = "token-issuer=${test.oauth2.tokenUri}")
@DisplayName("Oauth2TokenService — live integration smoke tests")
class Oauth2TokenServiceIT {

    @Autowired private Oauth2TokenService tokenService;

    // ── 1. Happy-path: token is non-blank ─────────────────────────────────────

    @Test
    @DisplayName("getAccessToken() returns a non-blank Bearer token from Entra")
    void getAccessToken_returnsNonBlankBearerToken() {
        String token = tokenService.getAccessToken();

        assertThat(token).as("Bearer token returned by Entra must not be blank").isNotBlank();
    }

    // ── 2. Token looks like a real JWT (3 dot-separated Base64 segments) ──────

    @Test
    @DisplayName("getAccessToken() returns a value that has the shape of a JWT")
    void getAccessToken_tokenHasJwtShape() {
        String token = tokenService.getAccessToken();

        String[] parts = token.split("\\.");
        assertThat(parts)
                .as(
                        "A JWT must contain exactly 3 dot-separated segments"
                                + " (header.payload.signature)")
                .hasSize(3);

        for (String part : parts) {
            assertThat(part)
                    .as("Each JWT segment must be a non-empty Base64url string")
                    .matches("[A-Za-z0-9_\\-]+=*");
        }
    }

    // ── 3. Token cache: second call returns the same cached token ─────────────

    @Test
    @DisplayName("getAccessToken() returns the same cached token on a second immediate call")
    void getAccessToken_secondCallReturnsCachedToken() {
        String firstToken = tokenService.getAccessToken();
        String secondToken = tokenService.getAccessToken();

        assertThat(secondToken)
                .as(
                        "Second call within the token TTL window must return the same cached"
                                + " token — no second round-trip to Entra")
                .isEqualTo(firstToken);
    }

    // ── 4. Explicit registration-ID overload works ────────────────────────────

    @Test
    @DisplayName("getAccessToken(registrationId) returns a non-blank token for 'uwb-default'")
    void getAccessToken_withExplicitRegistrationId_returnsToken() {
        String token = tokenService.getAccessToken(OAuth2ClientConfig.UWB_DEFAULT);

        assertThat(token)
                .as("Explicit registrationId='uwb-default' must also return a non-blank token")
                .isNotBlank();
    }

    // ── 5. POM registration returns a valid token ─────────────────────────────

    @Test
    @DisplayName("getAccessToken(POM_DEFAULT) returns a non-blank Bearer token from Entra")
    void getAccessToken_pomDefault_returnsNonBlankBearerToken() {
        String token = tokenService.getAccessToken(OAuth2ClientConfig.POM_DEFAULT);

        assertThat(token)
                .as(
                        "registrationId='pom-default' must return a non-blank token — "
                                + "confirms the POM ClientRegistration is wired in the repository")
                .isNotBlank();
    }

    // ── 6. UWB and POM tokens are cached independently ────────────────────────

    @Test
    @DisplayName(
            "getAccessToken() caches uwb-default and pom-default independently — "
                    + "each registration has its own cache entry")
    void getAccessToken_differentRegistrationIds_cachedIndependently() {
        String uwbToken1 = tokenService.getAccessToken(OAuth2ClientConfig.UWB_DEFAULT);
        String pomToken1 = tokenService.getAccessToken(OAuth2ClientConfig.POM_DEFAULT);

        assertThat(uwbToken1)
                .as("uwb-default: first call must return a non-blank token")
                .isNotBlank();
        assertThat(pomToken1)
                .as("pom-default: first call must return a non-blank token")
                .isNotBlank();

        String uwbToken2 = tokenService.getAccessToken(OAuth2ClientConfig.UWB_DEFAULT);
        String pomToken2 = tokenService.getAccessToken(OAuth2ClientConfig.POM_DEFAULT);

        assertThat(uwbToken2)
                .as(
                        "uwb-default: second immediate call must return the same cached token"
                                + " — no new round-trip to Entra")
                .isEqualTo(uwbToken1);
        assertThat(pomToken2)
                .as(
                        "pom-default: second immediate call must return the same cached token"
                                + " — no new round-trip to Entra")
                .isEqualTo(pomToken1);
    }
}
