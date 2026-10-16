package com.rukamini.mail;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pure unit tests for {@link SpringSecurityConfig} — no Spring context needed.
 * Covers role-helper beans; the filter-chain branches are covered by
 * {@link SpringSecurityConfigTest} integration tests.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SpringSecurityConfig — unit")
class SpringSecurityConfigUnitTest {

    private SpringSecurityConfig config;

    @BeforeEach
    void setUp() {
        config = new SpringSecurityConfig();
    }

    // ── role helper beans ─────────────────────────────────────────────────────

    @Test
    @DisplayName("allUserRoles() returns all base roles without Read/Write")
    void allUserRoles_containsBaseRolesOnly() {
        String[] roles = config.allUserRoles();
        assertThat(roles)
                .contains("IT_ADMIN", "READ_ONLY", "HEALTHCARE")
                .doesNotContain("Read", "Write");
    }

    @Test
    @DisplayName("allUserRolesWithReadWrite() appends Read and Write to base roles")
    void allUserRolesWithReadWrite_containsReadAndWrite() {
        String[] roles = config.allUserRolesWithReadWrite();
        assertThat(roles)
                .contains("Read", "Write", "IT_ADMIN")
                .hasSizeGreaterThan(config.allUserRoles().length);
    }

    @Test
    @DisplayName("USER_ROLES list is non-empty and contains expected roles")
    void userRoles_isNonEmpty() {
        assertThat(SpringSecurityConfig.USER_ROLES)
                .isNotEmpty()
                .contains("IT_ADMIN", "CATALOG_MANAGER_EMEA");
    }
}
