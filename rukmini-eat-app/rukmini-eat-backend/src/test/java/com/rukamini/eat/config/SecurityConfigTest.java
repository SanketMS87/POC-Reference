package com.rukamini.eat.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SecurityConfigTest {

    private static final String STRONG = "a-strong-production-secret-0123456789-abcdef";

    @Test
    void rejectsMissingOrShortSecretsInEveryProfile() {
        for (boolean local : new boolean[] {true, false}) {
            assertThrows(IllegalStateException.class, () -> SecurityConfig.validatedKey("", local));
            assertThrows(IllegalStateException.class, () -> SecurityConfig.validatedKey("short-secret", local));
            assertThrows(IllegalStateException.class,
                    () -> SecurityConfig.validatedKey("x".repeat(31), local));
        }
        assertEquals("HmacSHA256", SecurityConfig.validatedKey("x".repeat(32), false).getAlgorithm());
    }

    @Test
    void publicDevelopmentSecretIsOnlyAcceptedWithTheLocalProfile() {
        assertEquals("HmacSHA256", SecurityConfig.validatedKey(SecurityConfig.DEV_JWT_SECRET, true).getAlgorithm());
        var error = assertThrows(IllegalStateException.class,
                () -> SecurityConfig.validatedKey(SecurityConfig.DEV_JWT_SECRET, false));
        assertTrue(error.getMessage().contains("local profile"));
    }

    @Test
    void strongSecretsAreAcceptedOutsideTheLocalProfile() {
        assertEquals(STRONG.length(), SecurityConfig.validatedKey(STRONG, false).getEncoded().length);
    }
}
