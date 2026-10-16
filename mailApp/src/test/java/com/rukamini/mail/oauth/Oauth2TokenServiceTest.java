package com.rukamini.mail.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType;

@ExtendWith(MockitoExtension.class)
@DisplayName("Oauth2TokenService")
class Oauth2TokenServiceTest {

    @Mock private OAuth2AuthorizedClientManager authorizedClientManager;
    @Mock private OAuth2AuthorizedClient authorizedClient;

    private Oauth2TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new Oauth2TokenService(authorizedClientManager);
    }

    // ── happy-path ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAccessToken() returns token value from manager for uwb-default registration")
    void getAccessToken_noArg_returnsTokenForUwbDefault() {
        OAuth2AccessToken accessToken = buildToken("test-token-value");
        when(authorizedClient.getAccessToken()).thenReturn(accessToken);
        when(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class)))
                .thenReturn(authorizedClient);

        String result = tokenService.getAccessToken();

        assertThat(result).isEqualTo("test-token-value");
    }

    @Test
    @DisplayName("getAccessToken(registrationId) sends request with the given registration ID")
    void getAccessToken_withRegistrationId_usesCorrectRegistrationId() {
        OAuth2AccessToken accessToken = buildToken("token-for-custom-reg");
        when(authorizedClient.getAccessToken()).thenReturn(accessToken);
        when(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class)))
                .thenReturn(authorizedClient);

        String result = tokenService.getAccessToken("custom-reg");

        assertThat(result).isEqualTo("token-for-custom-reg");

        ArgumentCaptor<OAuth2AuthorizeRequest> captor =
                ArgumentCaptor.forClass(OAuth2AuthorizeRequest.class);
        verify(authorizedClientManager).authorize(captor.capture());
        assertThat(captor.getValue().getClientRegistrationId()).isEqualTo("custom-reg");
    }

    @Test
    @DisplayName("getAccessToken() no-arg delegates to uwb-default registration")
    void getAccessToken_noArg_delegatesToUwbDefault() {
        OAuth2AccessToken accessToken = buildToken("uwb-token");
        when(authorizedClient.getAccessToken()).thenReturn(accessToken);
        when(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class)))
                .thenReturn(authorizedClient);

        tokenService.getAccessToken();

        ArgumentCaptor<OAuth2AuthorizeRequest> captor =
                ArgumentCaptor.forClass(OAuth2AuthorizeRequest.class);
        verify(authorizedClientManager).authorize(captor.capture());
        assertThat(captor.getValue().getClientRegistrationId())
                .isEqualTo(OAuth2ClientConfig.UWB_DEFAULT);
    }

    // ── error cases ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAccessToken() throws IllegalStateException when manager returns null client")
    void getAccessToken_managerReturnsNull_throwsIllegalState() {
        when(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class))).thenReturn(null);

        assertThatThrownBy(() -> tokenService.getAccessToken())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("uwb-default")
                .hasMessageContaining("did not return an access token");
    }

    @Test
    @DisplayName("getAccessToken() throws IllegalStateException when accessToken is null")
    void getAccessToken_accessTokenNull_throwsIllegalState() {
        when(authorizedClient.getAccessToken()).thenReturn(null);
        when(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class)))
                .thenReturn(authorizedClient);

        assertThatThrownBy(() -> tokenService.getAccessToken())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("uwb-default")
                .hasMessageContaining("did not return an access token");
    }

    @Test
    @DisplayName("getAccessToken() throws IllegalStateException when token value is blank")
    void getAccessToken_tokenValueBlank_throwsIllegalState() {
        OAuth2AccessToken blankToken = mock(OAuth2AccessToken.class);
        when(blankToken.getTokenValue()).thenReturn("   ");
        when(authorizedClient.getAccessToken()).thenReturn(blankToken);
        when(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class)))
                .thenReturn(authorizedClient);

        assertThatThrownBy(() -> tokenService.getAccessToken())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("access token is blank")
                .hasMessageContaining("uwb-default");
    }

    @Test
    @DisplayName("getAccessToken(registrationId) propagates registration ID in error message")
    void getAccessToken_withRegistrationId_errorMessageContainsRegistrationId() {
        when(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class))).thenReturn(null);

        assertThatThrownBy(() -> tokenService.getAccessToken("some-other-reg"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("some-other-reg");
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private static OAuth2AccessToken buildToken(String tokenValue) {
        return new OAuth2AccessToken(
                TokenType.BEARER, tokenValue, Instant.now(), Instant.now().plusSeconds(3600));
    }
}
