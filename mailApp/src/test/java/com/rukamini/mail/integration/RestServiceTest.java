package com.rukamini.mail.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rukamini.mail.oauth.Oauth2TokenService;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

/**
 * Unit tests for {@link RestService}.
 *
 * <p>Failure paths are tested with an unreachable host (IOException → FAILURE branch).
 * Success paths are tested with a lightweight {@link HttpServer} bound to localhost,
 * serving a canned 200 response — no network dependency, no WireMock required.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RestService")
class RestServiceTest {

    @Mock private Oauth2TokenService oauth2TokenService;

    private RestService restService;
    private HttpServer localServer;
    private String localUrl;

    @BeforeEach
    void setUp() throws IOException {
        restService = new RestService(oauth2TokenService, new RestTemplate());

        // Bind to a random free port on loopback
        localServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = localServer.getAddress().getPort();
        localUrl = "http://127.0.0.1:" + port;
    }

    @AfterEach
    void tearDown() {
        if (localServer != null) {
            localServer.stop(0);
        }
    }

    // ── getCallOAuth2 — failure path ─────────────────────────────────────────

    @Test
    @DisplayName("getCallOAuth2 calls oauth2TokenService.getAccessToken with given registrationId")
    void getCallOAuth2_callsTokenServiceWithRegistrationId() {
        when(oauth2TokenService.getAccessToken("uwb-default")).thenReturn("test-bearer-token");

        Map<String, String> result =
                restService.getCallOAuth2(
                        "http://invalid.host.that.does.not.exist.local/api", "uwb-default");

        assertThat(result).containsKey("FAILURE");
    }

    @Test
    @DisplayName("getCallOAuth2 returns FAILURE map when URL is unreachable")
    void getCallOAuth2_unreachableUrl_returnsFailureMap() {
        when(oauth2TokenService.getAccessToken("pom-default")).thenReturn("some-token");

        Map<String, String> result =
                restService.getCallOAuth2(
                        "http://invalid.host.that.does.not.exist.local/api", "pom-default");

        assertThat(result).containsKey("FAILURE").doesNotContainKey("SUCCESS");
    }

    // ── getCallOAuth2 — invalid URI syntax ────────────────────────────────────

    @Test
    @DisplayName(
            "getCallOAuth2 returns FAILURE map when URL has invalid URI syntax (URISyntaxException path)")
    void getCallOAuth2_invalidUriSyntax_returnsFailureMap() {
        // URISyntaxException is thrown by URIBuilder before getAccessToken is called,
        // so no stub is needed — confirms the catch block handles it correctly.
        Map<String, String> result =
                restService.getCallOAuth2("http://invalid host/api", "uwb-default");
        assertThat(result).containsKey("FAILURE").doesNotContainKey("SUCCESS");
    }

    // ── getCallOAuth2 — success path ─────────────────────────────────────────

    @Test
    @DisplayName("getCallOAuth2 returns SUCCESS map with body when server returns 200")
    void getCallOAuth2_successResponse_returnsSuccessMap() {
        when(oauth2TokenService.getAccessToken("uwb-default")).thenReturn("test-token");

        byte[] body = "{\"data\":\"result\"}".getBytes(StandardCharsets.UTF_8);
        localServer.createContext(
                "/oauth-api",
                exchange -> {
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                    exchange.close();
                });
        localServer.start();

        Map<String, String> result =
                restService.getCallOAuth2(localUrl + "/oauth-api", "uwb-default");

        assertThat(result).containsKey("SUCCESS");
        assertThat(result.get("SUCCESS")).contains("result");
    }

    @Test
    @DisplayName(
            "getCallOAuth2 returns SUCCESS with empty body when server returns 204 No Content (entity null guard)")
    void getCallOAuth2_noContentResponse_returnsSuccessWithEmptyBody() {
        when(oauth2TokenService.getAccessToken("uwb-default")).thenReturn("test-token");

        localServer.createContext(
                "/no-content",
                exchange -> {
                    exchange.sendResponseHeaders(204, -1); // -1 signals no body
                    exchange.close();
                });
        localServer.start();

        Map<String, String> result =
                restService.getCallOAuth2(localUrl + "/no-content", "uwb-default");

        assertThat(result).containsKey("SUCCESS");
        assertThat(result.get("SUCCESS")).isEmpty();
    }

    // ── getCallOAuth2 — 404 path ──────────────────────────────────────────────

    @Test
    @DisplayName("getCallOAuth2 returns SUCCESS map even when server returns 404 with JSON body")
    void getCallOAuth2_404ResponseWithJsonBody_returnsSuccessMap() {
        when(oauth2TokenService.getAccessToken("uwb-default")).thenReturn("tok");
        String json404 = "{\"message\":\"Resource not found\",\"code\":404}";
        byte[] body = json404.getBytes(StandardCharsets.UTF_8);
        localServer.createContext(
                "/oauth-not-found",
                exchange -> {
                    exchange.sendResponseHeaders(404, body.length);
                    exchange.getResponseBody().write(body);
                    exchange.close();
                });
        localServer.start();

        Map<String, String> result =
                restService.getCallOAuth2(localUrl + "/oauth-not-found", "uwb-default");

        assertThat(result).containsKey("SUCCESS");
        assertThat(result.get("SUCCESS")).contains("Resource not found");
    }

    // ── getCallOAuth2 — token service called with correct registration id ──────

    @Test
    @DisplayName("getCallOAuth2 invokes token service with the exact registrationId passed")
    void getCallOAuth2_passesRegistrationIdToTokenService() {
        when(oauth2TokenService.getAccessToken("pom-default")).thenReturn("pom-token");

        restService.getCallOAuth2(
                "http://invalid.host.that.does.not.exist.local/api", "pom-default");

        verify(oauth2TokenService).getAccessToken("pom-default");
    }

    // ── getCallOAuth2 — Bearer token header correctness ──────────────────────

    @Test
    @DisplayName("getCallOAuth2 sends Bearer token header to the server")
    void getCallOAuth2_bearerTokenHeaderPresent_successReturned() {
        when(oauth2TokenService.getAccessToken("uwb-default")).thenReturn("my-access-token");

        AtomicReference<String> capturedAuth = new AtomicReference<>();
        byte[] body = "bearer-ok".getBytes(StandardCharsets.UTF_8);
        localServer.createContext(
                "/bearer-check",
                exchange -> {
                    capturedAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                    exchange.close();
                });
        localServer.start();

        Map<String, String> result =
                restService.getCallOAuth2(localUrl + "/bearer-check", "uwb-default");

        assertThat(result).containsKey("SUCCESS");
        assertThat(capturedAuth.get()).isEqualTo("Bearer my-access-token");
    }

    // ── getCallOAuth2(typed) — success path ───────────────────────────────────

    /** Simple record used to test typed deserialisation in tests. */
    record TestPayload(String name, int value) {}

    @Test
    @DisplayName("getCallOAuth2 typed overload deserialises JSON body into the requested class")
    void getCallOAuth2_typed_successResponse_deserialisesBody() {
        when(oauth2TokenService.getAccessToken("uwb-default")).thenReturn("test-token");

        byte[] body = "{\"name\":\"hello\",\"value\":42}".getBytes(StandardCharsets.UTF_8);
        localServer.createContext(
                "/typed-ok",
                exchange -> {
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                    exchange.close();
                });
        localServer.start();

        Optional<TestPayload> result =
                restService.getCallOAuth2(localUrl + "/typed-ok", "uwb-default", TestPayload.class);

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("hello");
        assertThat(result.get().value()).isEqualTo(42);
    }

    // ── getCallOAuth2(typed) — failure path ───────────────────────────────────

    @Test
    @DisplayName("getCallOAuth2 typed overload returns Optional.empty() when host is unreachable")
    void getCallOAuth2_typed_unreachableUrl_returnsEmpty() {
        when(oauth2TokenService.getAccessToken("uwb-default")).thenReturn("tok");

        Optional<TestPayload> result =
                restService.getCallOAuth2(
                        "http://invalid.host.that.does.not.exist.local/api",
                        "uwb-default",
                        TestPayload.class);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getCallOAuth2 typed overload returns Optional.empty() on 4xx response")
    void getCallOAuth2_typed_4xxResponse_returnsEmpty() {
        when(oauth2TokenService.getAccessToken("uwb-default")).thenReturn("tok");

        localServer.createContext(
                "/typed-404",
                exchange -> {
                    exchange.sendResponseHeaders(404, -1);
                    exchange.close();
                });
        localServer.start();

        Optional<TestPayload> result =
                restService.getCallOAuth2(
                        localUrl + "/typed-404", "uwb-default", TestPayload.class);

        assertThat(result).isEmpty();
    }
}
