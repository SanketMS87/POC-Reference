package com.rukamini.mail.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;

/**
 * Unit tests for {@link RestApplication}.
 *
 * <p>Property-backed bean definitions live in {@link AppConfig} (see its dedicated test class);
 * this class only tests what remains in {@link RestApplication} itself.
 */
@DisplayName("RestApplication")
class RestApplicationTest {

    private RestApplication app;

    @BeforeEach
    void setUp() {
        app = new RestApplication();
    }

    // ── exceptionTranslation ──────────────────────────────────────────────────

    @Test
    @DisplayName("exceptionTranslation returns PersistenceExceptionTranslationPostProcessor")
    void exceptionTranslation_returnsPostProcessor() {
        assertThat(app.exceptionTranslation())
                .isInstanceOf(PersistenceExceptionTranslationPostProcessor.class);
    }
}
