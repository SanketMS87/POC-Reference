package com.rukamini.eat.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class SecurityApiIntegrationTest extends ApiTestSupport {

    @Autowired
    private SecretKey jwtSecretKey;

    private String token(SecretKey key, String subject, List<String> roles, Instant issuedAt, Instant expiresAt) {
        return token(key, "rukmini-eat", subject, roles, issuedAt, expiresAt);
    }

    private String token(SecretKey key, String issuer, String subject, List<String> roles, Instant issuedAt,
                         Instant expiresAt) {
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        JwtClaimsSet.Builder builder = JwtClaimsSet.builder().subject(subject)
                .issuedAt(issuedAt).expiresAt(expiresAt).claim("roles", roles);
        if (issuer != null) {
            builder.issuer(issuer);
        }
        JwtClaimsSet claims = builder.build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    @Test
    void tokensFromAnotherIssuerAreRejectedEvenWhenCorrectlySigned() throws Exception {
        Instant now = Instant.now();
        String wrongIssuer = token(jwtSecretKey, "someone-else", "admin@rukmini.com", List.of("ADMIN"), now,
                now.plus(1, ChronoUnit.HOURS));
        assertEquals(401, call("GET", "/api/restaurants", null, wrongIssuer).status());
        assertEquals(401, call("POST", "/api/admin/restaurants", "{}", wrongIssuer).status());
        String noIssuer = token(jwtSecretKey, null, "admin@rukmini.com", List.of("ADMIN"), now,
                now.plus(1, ChronoUnit.HOURS));
        assertEquals(401, call("GET", "/api/restaurants", null, noIssuer).status());
    }

    private SecretKey otherKey() {
        return new SecretKeySpec("a-completely-different-secret-key-0123456789".getBytes(StandardCharsets.UTF_8),
                "HmacSHA256");
    }

    @Test
    void tokensSignedWithTheServerKeyAreAccepted() throws Exception {
        String email = uniqueEmail();
        call("POST", "/api/auth/register", registerBody("Signed", email, PASSWORD), null);
        Instant now = Instant.now();
        String token = token(jwtSecretKey, email, List.of("CUSTOMER"), now, now.plus(5, ChronoUnit.MINUTES));
        assertEquals(200, call("GET", "/api/orders", null, token).status());
    }

    @Test
    void expiredTokensAreRejected() throws Exception {
        Instant now = Instant.now();
        String token = token(jwtSecretKey, "demo@rukmini.com", List.of("CUSTOMER"),
                now.minus(2, ChronoUnit.HOURS), now.minus(1, ChronoUnit.HOURS));
        assertEquals(401, call("GET", "/api/restaurants", null, token).status());
    }

    @Test
    void tokensSignedWithAnotherKeyAreRejectedEvenWithAdminRole() throws Exception {
        Instant now = Instant.now();
        String forged = token(otherKey(), "admin@rukmini.com", List.of("ADMIN"), now, now.plus(1, ChronoUnit.HOURS));
        assertEquals(401, call("GET", "/api/restaurants", null, forged).status());
        assertEquals(401, call("POST", "/api/admin/restaurants", "{}", forged).status());
    }

    @Test
    void tamperedTokensAreRejected() throws Exception {
        String valid = newCustomerToken();
        String[] parts = valid.split("\\.");
        String swappedSignature = parts[0] + "." + parts[1] + "." + "A".repeat(parts[2].length());
        assertEquals(401, call("GET", "/api/orders", null, swappedSignature).status());
        assertEquals(401, call("GET", "/api/orders", null, valid + "x").status());
        assertEquals(401, call("GET", "/api/orders", null, "not.a.jwt").status());
        assertEquals(401, call("GET", "/api/orders", null, "").status());
    }

    @Test
    void tokensWithoutTheAdminRoleCannotAdminister() throws Exception {
        Instant now = Instant.now();
        String noRoles = token(jwtSecretKey, "admin@rukmini.com", List.of(), now, now.plus(1, ChronoUnit.HOURS));
        assertEquals(403, call("POST", "/api/admin/restaurants", "{}", noRoles).status());
    }

    @Test
    void validTokenForAnUnknownUserIsRejectedByOrderEndpoints() throws Exception {
        Instant now = Instant.now();
        String ghost = token(jwtSecretKey, "ghost-" + uniqueEmail(), List.of("CUSTOMER"), now,
                now.plus(1, ChronoUnit.HOURS));
        var history = call("GET", "/api/orders", null, ghost);
        assertEquals(401, history.status());
        assertEquals("User not found", history.message());
        assertEquals(401, call("POST", "/api/orders", orderBody(1, 1), ghost).status());
    }

    @Test
    void onlyTheBearerSchemeIsAccepted() throws Exception {
        String token = newCustomerToken();
        for (String header : new String[] {"Basic " + token, token, "Token " + token}) {
            HttpResponse<String> response = raw(HttpRequest.newBuilder(uri("/api/orders"))
                    .header("Authorization", header).GET());
            assertEquals(401, response.statusCode(), header.substring(0, 5));
        }
    }

    @Test
    void unknownPathsRequireAuthenticationBeforeBeingReportedMissing() throws Exception {
        assertEquals(401, call("GET", "/api/does-not-exist", null, null).status());
        assertEquals(404, call("GET", "/api/does-not-exist", null, newCustomerToken()).status());
    }

    @Test
    void unsupportedMethodsAndMediaTypesAreReportedAccurately() throws Exception {
        String token = newCustomerToken();
        assertEquals(405, call("DELETE", "/api/restaurants", null, token).status());
        assertEquals(405, call("PUT", "/api/orders", "{}", token).status());
        assertEquals(405, call("GET", "/api/auth/login", null, null).status());
        HttpResponse<String> noContentType = raw(HttpRequest.newBuilder(uri("/api/auth/login"))
                .POST(HttpRequest.BodyPublishers.ofString(loginBody("demo@rukmini.com", "Demo@123"))));
        assertEquals(415, noContentType.statusCode());
    }

    @Test
    void corsAllowsOnlyTheConfiguredFrontendOrigin() throws Exception {
        HttpResponse<String> allowed = raw(HttpRequest.newBuilder(uri("/api/orders"))
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "authorization,content-type")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody()));
        assertEquals(200, allowed.statusCode());
        assertEquals("http://localhost:4200",
                allowed.headers().firstValue("Access-Control-Allow-Origin").orElse(null));
        assertTrue(allowed.headers().firstValue("Access-Control-Allow-Methods").orElse("").contains("POST"));

        HttpResponse<String> denied = raw(HttpRequest.newBuilder(uri("/api/orders"))
                .header("Origin", "http://evil.example")
                .header("Access-Control-Request-Method", "POST")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody()));
        assertEquals(403, denied.statusCode());
        assertTrue(denied.headers().firstValue("Access-Control-Allow-Origin").isEmpty());
    }

    @Test
    void actualResponsesCarryCorsHeadersOnlyForTheFrontendOrigin() throws Exception {
        String token = newCustomerToken();
        HttpResponse<String> fromFrontend = raw(HttpRequest.newBuilder(uri("/api/restaurants"))
                .header("Authorization", "Bearer " + token).header("Origin", "http://localhost:4200").GET());
        assertEquals(200, fromFrontend.statusCode());
        assertEquals("http://localhost:4200",
                fromFrontend.headers().firstValue("Access-Control-Allow-Origin").orElse(null));

        HttpResponse<String> fromElsewhere = raw(HttpRequest.newBuilder(uri("/api/restaurants"))
                .header("Authorization", "Bearer " + token).header("Origin", "http://evil.example").GET());
        assertEquals(403, fromElsewhere.statusCode());
    }

    @Test
    void responsesAreStatelessAndSetNoSessionCookie() throws Exception {
        HttpResponse<String> response = raw(HttpRequest.newBuilder(uri("/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(loginBody("demo@rukmini.com", "Demo@123"))));
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().allValues("Set-Cookie").isEmpty());
        assertNotNull(response.headers().firstValue("X-Content-Type-Options").orElse(null));
        assertFalse(response.body().isBlank());
    }
}
