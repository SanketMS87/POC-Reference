package com.rukamini.eat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.rukamini.eat.domain.Role;
import com.rukamini.eat.domain.User;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {

    private static final SecretKey KEY = new SecretKeySpec(
            "unit-test-secret-key-0123456789abcdef".getBytes(StandardCharsets.UTF_8), "HmacSHA256");

    private Jwt decode(String token) {
        return NimbusJwtDecoder.withSecretKey(KEY).macAlgorithm(MacAlgorithm.HS256).build().decode(token);
    }

    private JwtService service(long ttlMinutes) {
        return new JwtService(new NimbusJwtEncoder(new ImmutableSecret<>(KEY)), ttlMinutes);
    }

    private User userWithId(String email, Role role) {
        User user = new User("Asha", email, "hash", role);
        ReflectionTestUtils.setField(user, "id", 42L);
        return user;
    }

    @Test
    void tokenCarriesIssuerSubjectAndRoles() {
        Jwt jwt = decode(service(60).generate(userWithId("asha@example.com", Role.CUSTOMER)));
        assertEquals("rukmini-eat", jwt.getClaimAsString("iss"));
        assertEquals("asha@example.com", jwt.getSubject());
        assertEquals(List.of("CUSTOMER"), jwt.getClaimAsStringList("roles"));
        assertEquals(42L, ((Number) jwt.getClaim("uid")).longValue());
    }

    @Test
    void adminRoleIsEncodedInTheToken() {
        Jwt jwt = decode(service(60).generate(userWithId("admin@example.com", Role.ADMIN)));
        assertEquals(List.of("ADMIN"), jwt.getClaimAsStringList("roles"));
    }

    @Test
    void expiryFollowsTheConfiguredTimeToLive() {
        Jwt jwt = decode(service(15).generate(userWithId("asha@example.com", Role.CUSTOMER)));
        Instant issuedAt = Objects.requireNonNull(jwt.getIssuedAt());
        Instant expiresAt = Objects.requireNonNull(jwt.getExpiresAt());
        assertEquals(Duration.ofMinutes(15), Duration.between(issuedAt, expiresAt));
        assertTrue(expiresAt.isAfter(Instant.now()));
    }

    @Test
    void tokensAreSignedWithTheSecretKeyOnly() {
        String token = service(60).generate(userWithId("asha@example.com", Role.CUSTOMER));
        SecretKey wrong = new SecretKeySpec("another-secret-key-0123456789abcdefgh".getBytes(StandardCharsets.UTF_8),
                "HmacSHA256");
        var decoder = NimbusJwtDecoder.withSecretKey(wrong).macAlgorithm(MacAlgorithm.HS256).build();
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.security.oauth2.jwt.JwtException.class,
                () -> decoder.decode(token));
    }
}
