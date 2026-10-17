package com.rukamini.eat.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("local")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:rate-limit-tests;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.security.rate-limit.max-attempts=3",
        "app.security.rate-limit.window-seconds=600"
})
class RateLimitIntegrationTest extends ApiTestSupport {

    @Test
    void loginIsThrottledAfterTheConfiguredNumberOfAttempts() throws Exception {
        String body = loginBody("demo@rukmini.com", "wrong-password");
        for (int i = 0; i < 3; i++) {
            assertEquals(401, call("POST", "/api/auth/login", body, null).status());
        }
        HttpResponse<String> throttled = raw(HttpRequest.newBuilder(uri("/api/auth/login"))
                .header("Content-Type", "application/json")
                .header("Origin", "http://localhost:4200")
                .POST(HttpRequest.BodyPublishers.ofString(body)));
        assertEquals(429, throttled.statusCode());
        assertTrue(Long.parseLong(throttled.headers().firstValue("Retry-After").orElseThrow()) > 0);
        assertEquals("Too many attempts. Try again later.", mapper.readTree(throttled.body()).path("message").asText());
        assertEquals("http://localhost:4200",
                throttled.headers().firstValue("Access-Control-Allow-Origin").orElse(null));

        var correctCredentials = call("POST", "/api/auth/login", loginBody("demo@rukmini.com", "Demo@123"), null);
        assertEquals(429, correctCredentials.status(), "throttling applies even to valid credentials");
    }

    @Test
    void registrationHasItsOwnCounterAndOtherEndpointsAreNotThrottled() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertEquals(201, call("POST", "/api/auth/register",
                    registerBody("Rate test", uniqueEmail(), PASSWORD), null).status());
        }
        assertEquals(429, call("POST", "/api/auth/register",
                registerBody("Rate test", uniqueEmail(), PASSWORD), null).status());

        HttpResponse<String> preflight = raw(HttpRequest.newBuilder(uri("/api/auth/login"))
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "POST")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody()));
        assertEquals(200, preflight.statusCode());
        for (int i = 0; i < 6; i++) {
            assertEquals(401, call("GET", "/api/restaurants", null, null).status());
        }
        assertFalse(call("GET", "/v3/api-docs", null, null).body().isBlank());
    }
}
