package com.rukamini.eat.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Verifies that development conveniences are absent when the "local" profile is not active. */
@ActiveProfiles(profiles = "prod", inheritProfiles = false)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:prod-profile-tests;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.jwt.secret=a-strong-production-secret-0123456789-abcdef",
        "app.security.rate-limit.max-attempts=0"
})
class NonLocalProfileIntegrationTest extends ApiTestSupport {

    @Test
    void demoAccountsAndSampleRestaurantsAreNotSeeded() throws Exception {
        assertEquals(401, call("POST", "/api/auth/login", loginBody("demo@rukmini.com", "Demo@123"), null).status());
        assertEquals(401, call("POST", "/api/auth/login", loginBody("admin@rukmini.com", "Admin@123"), null).status());
        assertTrue(restaurants(newCustomerToken(), "").isEmpty());
    }

    @Test
    void swaggerAndDatabaseConsoleAreNotExposed() throws Exception {
        assertEquals(404, call("GET", "/v3/api-docs", null, null).status());
        assertEquals(404, call("GET", "/swagger-ui/index.html", null, null).status());
        String token = newCustomerToken();
        assertEquals(404, call("GET", "/h2-console", null, token).status());
    }

    @Test
    void registrationLoginAndProtectedCallsStillWork() throws Exception {
        String email = uniqueEmail();
        assertEquals(201, call("POST", "/api/auth/register", registerBody("Prod user", email, PASSWORD), null).status());
        String token = loginToken(email, PASSWORD);
        assertEquals(200, call("GET", "/api/orders", null, token).status());
        assertEquals(403, call("POST", "/api/admin/restaurants", "{}", token).status());
    }
}
