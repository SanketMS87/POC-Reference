package com.rukamini.mail.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/** Verifies {@code @ConfigurationProperties} binding, validation and secret placeholders. */
@DisplayName("AppProperties / SecurityProperties binding")
class AppPropertiesBindingTest {

    @Configuration
    @EnableConfigurationProperties({AppProperties.class, SecurityProperties.class})
    static class TestConfig {}

    private static final String[] REQUIRED = {
        "app.environment=TEST",
        "app.default-schema=cwf",
        "app.datasource.cwf.url=jdbc:postgresql://db:5432/cwf",
        "app.mail.host=smtp",
        "app.mail.sender-address=a@b.c",
        "app.urls.pom=p",
        "app.urls.deal=d",
        "app.urls.deal-app=da",
        "app.urls.renewal-overview=r",
        "app.oauth2.uwb-scope=u",
        "app.oauth2.pom-scope=p",
        "app.oauth2.graph-scope=g",
        "app.security.jwt.jwk-set-uri=https://issuer/keys",
        "app.security.jwt.issuer=https://issuer",
        "app.security.jwt.audiences=a1,a2",
    };

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withUserConfiguration(TestConfig.class);

    @Test
    @DisplayName("binds app.* and app.security.* with defaults")
    void bindsProperties() {
        runner.withPropertyValues(REQUIRED)
                .run(
                        ctx -> {
                            AppProperties app = ctx.getBean(AppProperties.class);
                            assertThat(app.getEnvironment()).isEqualTo("TEST");
                            assertThat(app.getDatasource().getCwf().getUrl())
                                    .isEqualTo("jdbc:postgresql://db:5432/cwf");
                            assertThat(app.getDatasource().getCwf().getDriverClassName())
                                    .isEqualTo("org.postgresql.Driver");
                            assertThat(app.getDatasource().getCwf().getPool().getMaxSize())
                                    .isEqualTo(50);
                            assertThat(app.getMail().getSmtp().isStarttls()).isTrue();
                            SecurityProperties sec = ctx.getBean(SecurityProperties.class);
                            assertThat(sec.getJwt().getAudiences()).containsExactly("a1", "a2");
                            assertThat(sec.isGrantAllRolesToAuthenticated()).isFalse();
                        });
    }

    @Test
    @DisplayName("secret placeholder prefers the Key Vault secret and falls back to the env value")
    void secretPlaceholderPrefersKeyVault() {
        runner.withPropertyValues(REQUIRED)
                .withPropertyValues(
                        "app.datasource.cwf.password=${cwf-db-password:${DB_FALLBACK:fallback}}")
                .run(
                        ctx ->
                                assertThat(
                                                ctx.getBean(AppProperties.class)
                                                        .getDatasource()
                                                        .getCwf()
                                                        .getPassword())
                                        .isEqualTo("fallback"));
        runner.withPropertyValues(REQUIRED)
                .withPropertyValues(
                        "app.datasource.cwf.password=${cwf-db-password:${DB_FALLBACK:fallback}}",
                        "cwf-db-password=from-key-vault")
                .run(
                        ctx ->
                                assertThat(
                                                ctx.getBean(AppProperties.class)
                                                        .getDatasource()
                                                        .getCwf()
                                                        .getPassword())
                                        .isEqualTo("from-key-vault"));
    }

    @Test
    @DisplayName("startup fails fast when the datasource url is missing")
    void failsWithoutDatasourceUrl() {
        runner.withPropertyValues(
                        "app.environment=TEST",
                        "app.default-schema=cwf",
                        "app.mail.host=smtp",
                        "app.mail.sender-address=a@b.c",
                        "app.urls.pom=p",
                        "app.urls.deal=d",
                        "app.urls.deal-app=da",
                        "app.urls.renewal-overview=r",
                        "app.oauth2.uwb-scope=u",
                        "app.oauth2.pom-scope=p",
                        "app.oauth2.graph-scope=g")
                .run(ctx -> assertThat(ctx).hasFailed());
    }
}
