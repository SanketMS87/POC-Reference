package com.rukamini.mail;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.test.context.ActiveProfiles;

/**
 * Spring context integration tests for {@link SpringSecurityConfig}.
 *
 * <p>CSRF is intentionally disabled (Sonar S4502).
 * Rationale: this is a stateless REST API authenticated with bearer tokens (JWT resource
 * server). Spring Security never issues session cookies, so there is nothing for a
 * cross-site request to forge. Disabling CSRF is safe and correct here.
 */
class SpringSecurityConfigTest {

    // ── local profile ─────────────────────────────────────────────────────────

    @Nested
    @SpringBootTest(
            classes = SpringSecurityConfig.class,
            webEnvironment = WebEnvironment.MOCK,
            properties = "spring.security.filter.order=10")
    @ActiveProfiles("local")
    @DisplayName("local profile — permit-all, no CSRF")
    class LocalProfileTest {

        private final FilterChainProxy filterChainProxy;

        @Autowired
        LocalProfileTest(FilterChainProxy filterChainProxy) {
            this.filterChainProxy = filterChainProxy;
        }

        @Test
        @DisplayName("FilterChainProxy is wired in the application context")
        void filterChainProxy_isPresent() {
            assertThat(filterChainProxy).isNotNull();
        }

        @Test
        @DisplayName(
                "CsrfFilter is absent — CSRF disabled intentionally for stateless bearer-token REST API")
        void csrfFilter_isNotPresent() {
            boolean hasCsrf =
                    filterChainProxy.getFilterChains().stream()
                            .flatMap(fc -> fc.getFilters().stream())
                            .anyMatch(CsrfFilter.class::isInstance);
            assertThat(hasCsrf)
                    .as(
                            "CsrfFilter must not be present: stateless bearer-token API has no session cookies to protect")
                    .isFalse();
        }
    }

    // ── non-local profile (production path) ──────────────────────────────────

    @Nested
    @SpringBootTest(
            classes = {
                SpringSecurityConfig.class,
                org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport.class
            },
            webEnvironment = WebEnvironment.MOCK,
            properties = {
                "spring.security.filter.order=10",
                "spring.cloud.azure.keyvault.secret.property-source-enabled=false",
                "spring.cloud.azure.keyvault.secret.property-sources[0].enabled=false"
            })
    @ActiveProfiles("prod")
    @DisplayName("non-local profile — authenticated routes, no CSRF")
    class ProdProfileTest {

        private final FilterChainProxy filterChainProxy;

        @Autowired
        ProdProfileTest(FilterChainProxy filterChainProxy) {
            this.filterChainProxy = filterChainProxy;
        }

        @Test
        @DisplayName("FilterChainProxy is wired for non-local profile")
        void filterChainProxy_isPresent() {
            assertThat(filterChainProxy).isNotNull();
        }

        @Test
        @DisplayName("CsrfFilter is absent in non-local profile too")
        void csrfFilter_isNotPresent() {
            boolean hasCsrf =
                    filterChainProxy.getFilterChains().stream()
                            .flatMap(fc -> fc.getFilters().stream())
                            .anyMatch(CsrfFilter.class::isInstance);
            assertThat(hasCsrf)
                    .as(
                            "CsrfFilter must not be present in prod: stateless bearer-token API has no session cookies")
                    .isFalse();
        }
    }
}
