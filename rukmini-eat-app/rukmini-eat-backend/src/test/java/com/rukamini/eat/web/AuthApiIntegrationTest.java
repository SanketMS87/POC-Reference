package com.rukamini.eat.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class AuthApiIntegrationTest extends ApiTestSupport {

    @Test
    void registrationNormalisesEmailAndReturnsCustomerWithoutSecrets() throws Exception {
        String email = uniqueEmail();
        var reply = call("POST", "/api/auth/register",
                registerBody("  Asha Rao  ", email.toUpperCase(), PASSWORD), null);
        assertEquals(201, reply.status());
        JsonNode user = reply.json().path("user");
        assertEquals(email.toLowerCase(), user.path("email").asText());
        assertEquals("Asha Rao", user.path("name").asText());
        assertEquals("CUSTOMER", user.path("role").asText());
        assertTrue(user.path("id").asLong() > 0);
        assertFalse(reply.body().contains("passwordHash"));
        assertFalse(reply.body().contains(PASSWORD));
        assertFalse(reply.json().path("token").asText().isBlank());
    }

    @Test
    void emailsWithSurroundingWhitespaceAreRejectedByValidation() throws Exception {
        var reply = call("POST", "/api/auth/register",
                registerBody("Asha", " " + uniqueEmail() + " ", PASSWORD), null);
        assertEquals(400, reply.status());
        assertTrue(reply.message().contains("email"), reply.body());
    }

    @Test
    void registrationRejectsInvalidInput() throws Exception {
        String ok = uniqueEmail();
        assertInvalid(registerBody("", ok, PASSWORD), "name");
        assertInvalid(registerBody("A".repeat(81), ok, PASSWORD), "name");
        assertInvalid(registerBody("Asha", "not-an-email", PASSWORD), "email");
        assertInvalid(registerBody("Asha", "", PASSWORD), "email");
        assertInvalid(registerBody("Asha", ok, "12345"), "password");
        assertInvalid(registerBody("Asha", ok, "p".repeat(73)), "password");
        assertInvalid("{}", ":");
    }

    private void assertInvalid(String body, String expectedFieldHint) throws Exception {
        var reply = call("POST", "/api/auth/register", body, null);
        assertEquals(400, reply.status(), body);
        assertTrue(reply.message().contains(expectedFieldHint), reply.body());
    }

    @Test
    void registrationAcceptsBoundaryLengths() throws Exception {
        assertEquals(201, call("POST", "/api/auth/register",
                registerBody("N".repeat(80), uniqueEmail(), "p".repeat(6)), null).status());
        assertEquals(201, call("POST", "/api/auth/register",
                registerBody("Asha", uniqueEmail(), "p".repeat(72)), null).status());
    }

    @Test
    void duplicateEmailIsRejectedCaseInsensitively() throws Exception {
        String email = uniqueEmail();
        assertEquals(201, call("POST", "/api/auth/register", registerBody("First", email, PASSWORD), null).status());
        var duplicate = call("POST", "/api/auth/register",
                registerBody("Second", email.toUpperCase(), PASSWORD), null);
        assertEquals(409, duplicate.status());
        assertEquals("Email is already registered", duplicate.message());
    }

    @Test
    void malformedOrMissingBodiesAreClientErrors() throws Exception {
        assertEquals(400, call("POST", "/api/auth/login", "{not json", null).status());
        assertEquals(400, call("POST", "/api/auth/register", "{not json", null).status());
        assertEquals(400, call("POST", "/api/auth/login", null, null).status());
    }

    @Test
    void loginIsCaseInsensitiveAndDoesNotRevealWhichPartWasWrong() throws Exception {
        String email = uniqueEmail();
        call("POST", "/api/auth/register", registerBody("Login user", email, PASSWORD), null);

        var ok = call("POST", "/api/auth/login", loginBody(email.toUpperCase(), PASSWORD), null);
        assertEquals(200, ok.status());
        assertEquals(email, ok.json().path("user").path("email").asText());

        var wrongPassword = call("POST", "/api/auth/login", loginBody(email, "Wrong-pass-1"), null);
        var unknownEmail = call("POST", "/api/auth/login", loginBody(uniqueEmail(), PASSWORD), null);
        assertEquals(401, wrongPassword.status());
        assertEquals(401, unknownEmail.status());
        assertEquals("Invalid email or password", wrongPassword.message());
        assertEquals(wrongPassword.message(), unknownEmail.message());
    }

    @Test
    void loginRequiresBothFields() throws Exception {
        assertEquals(400, call("POST", "/api/auth/login", loginBody("", PASSWORD), null).status());
        assertEquals(400, call("POST", "/api/auth/login", loginBody("demo@rukmini.com", ""), null).status());
        assertEquals(400, call("POST", "/api/auth/login", "{}", null).status());
    }

    @Test
    void passwordsAreCaseSensitive() throws Exception {
        assertEquals(401, call("POST", "/api/auth/login", loginBody("demo@rukmini.com", "demo@123"), null).status());
    }

    @Test
    void demoAccountsHaveExpectedRoles() throws Exception {
        var customer = call("POST", "/api/auth/login", loginBody("demo@rukmini.com", "Demo@123"), null);
        var admin = call("POST", "/api/auth/login", loginBody("admin@rukmini.com", "Admin@123"), null);
        assertEquals("CUSTOMER", customer.json().path("user").path("role").asText());
        assertEquals("ADMIN", admin.json().path("user").path("role").asText());
    }

    @Test
    void issuedTokenCarriesSubjectRoleAndFutureExpiry() throws Exception {
        String email = uniqueEmail();
        var registered = call("POST", "/api/auth/register", registerBody("Token user", email, PASSWORD), null);
        String token = registered.json().path("token").asText();
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);
        JsonNode claims = mapper.readTree(new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8));
        assertEquals(email, claims.path("sub").asText());
        assertEquals("rukmini-eat", claims.path("iss").asText());
        assertEquals("CUSTOMER", claims.path("roles").path(0).asText());
        assertEquals(registered.json().path("user").path("id").asLong(), claims.path("uid").asLong());
        assertEquals(3600, claims.path("exp").asLong() - claims.path("iat").asLong());
    }
}
