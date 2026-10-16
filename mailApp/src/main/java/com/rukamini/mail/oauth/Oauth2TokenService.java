package com.rukamini.mail.oauth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Component;

/**
 * Single entry point for acquiring outbound OAuth2 access tokens.
 *
 * <p>Delegates to Spring Security's {@link OAuth2AuthorizedClientManager}, which caches the token
 * in memory for its full {@code expires_in} window (~3600 s for Entra) and fetches a new one
 * automatically when it lapses — no manual token lifecycle management required.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Oauth2TokenService {

    /**
     * Synthetic principal name used for client-credentials grants (no real user is involved).
     * Must be a non-null, non-empty string to satisfy the Spring Security API contract.
     */
    private static final String SERVICE_PRINCIPAL = "service-account";

    private final OAuth2AuthorizedClientManager authorizedClientManager;

    /**
     * Returns a valid access token for the {@code uwb-default} registration.
     */
    public String getAccessToken() {
        return getAccessToken(OAuth2ClientConfig.UWB_DEFAULT);
    }

    /**
     * Returns a valid access token for the given {@code registrationId}.
     *
     * @param registrationId the Spring Security client registration ID (e.g. {@code "uwb-default"})
     * @throws IllegalStateException if the token endpoint does not return a token
     */
    public String getAccessToken(String registrationId) {
        log.debug(
                "Oauth2TokenService: acquiring access token for registration='{}'", registrationId);

        OAuth2AuthorizeRequest request =
                OAuth2AuthorizeRequest.withClientRegistrationId(registrationId)
                        .principal(SERVICE_PRINCIPAL)
                        .build();

        OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(request);

        if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
            log.error(
                    "Oauth2TokenService: token endpoint returned no access token for"
                            + " registration='{}' — check clientId/clientSecret/scope/tokenUri"
                            + " in OAuth2ClientConfig",
                    registrationId);
            throw new IllegalStateException(
                    "OAuth2 token endpoint did not return an access token for registration '"
                            + registrationId
                            + "'");
        }

        OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
        String tokenValue = accessToken.getTokenValue();

        if (tokenValue == null || tokenValue.isBlank()) {
            log.error(
                    "Oauth2TokenService: access token value is blank for registration='{}'",
                    registrationId);
            throw new IllegalStateException(
                    "OAuth2 access token is blank for registration '" + registrationId + "'");
        }

        if (log.isDebugEnabled()) {
            log.debug(
                    "Oauth2TokenService: access token acquired successfully for registration='{}',"
                            + " expiresAt='{}'",
                    registrationId,
                    accessToken.getExpiresAt());
        } else {
            log.info(
                    "Oauth2TokenService: access token acquired for registration='{}'",
                    registrationId);
        }

        return tokenValue;
    }
}
