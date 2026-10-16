package com.rukamini.mail.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/** Unit tests for {@link AppConfig} (property-backed beans, no Spring context needed). */
@DisplayName("AppConfig")
class AppConfigTest {

    private AppProperties props;
    private AppConfig config;

    @BeforeEach
    void setUp() {
        props = new AppProperties();
        props.setEnvironment("LOCAL");
        props.setDefaultSchema("cwf");
        AppProperties.Cwf cwf = props.getDatasource().getCwf();
        cwf.setUrl("jdbc:postgresql://localhost:5432/cwf");
        cwf.setUsername("user");
        cwf.setPassword("pass");
        AppProperties.Mail mail = props.getMail();
        mail.setHost("smtp.local");
        mail.setPort(2525);
        mail.setSenderAddress("a@b.c");
        mail.setTechUser("mu");
        mail.setTechPass("mp");
        AppProperties.Urls urls = props.getUrls();
        urls.setPom("pom");
        urls.setDeal("deal");
        urls.setDealApp("app");
        urls.setRenewalOverview("ren");
        AppProperties.OAuth2 oauth2 = props.getOauth2();
        oauth2.setSwisscomClient("c");
        oauth2.setSwisscomSecret("s");
        oauth2.setUwbScope("uwb");
        oauth2.setPomScope("pom-scope");
        oauth2.setGraphScope("graph");
        config = new AppConfig(props);
    }

    // ── javaMailProperties ────────────────────────────────────────────────────

    @Test
    @DisplayName("javaMailProperties reflects protocol, STARTTLS, auth and ssl trust")
    void javaMailProperties_reflectsFlags() {
        AppProperties.Smtp smtp = props.getMail().getSmtp();
        smtp.setAuth(true);
        smtp.setStarttls(true);
        smtp.setSslTrust("relay");
        Properties mailProps = config.javaMailProperties(smtp);
        assertThat(mailProps.getProperty("mail.transport.protocol")).isEqualTo("smtp");
        assertThat(mailProps.getProperty("mail.smtp.starttls.enable")).isEqualTo("true");
        assertThat(mailProps.getProperty("mail.smtp.auth")).isEqualTo("true");
        assertThat(mailProps.getProperty("mail.smtp.ssl.trust")).isEqualTo("relay");
    }

    @Test
    @DisplayName("javaMailProperties disables STARTTLS/auth and omits blank ssl trust")
    void javaMailProperties_disabledFlags() {
        AppProperties.Smtp smtp = props.getMail().getSmtp();
        smtp.setAuth(false);
        smtp.setStarttls(false);
        smtp.setSslTrust("");
        Properties mailProps = config.javaMailProperties(smtp);
        assertThat(mailProps.getProperty("mail.smtp.starttls.enable")).isEqualTo("false");
        assertThat(mailProps.getProperty("mail.smtp.auth")).isEqualTo("false");
        assertThat(mailProps.containsKey("mail.smtp.ssl.trust")).isFalse();
    }

    @Test
    @DisplayName("getJavaMailSender applies host, port and credentials")
    void getJavaMailSender_appliesSettings() {
        JavaMailSenderImpl sender = (JavaMailSenderImpl) config.getJavaMailSender("u", "p");
        assertThat(sender.getHost()).isEqualTo("smtp.local");
        assertThat(sender.getPort()).isEqualTo(2525);
        assertThat(sender.getUsername()).isEqualTo("u");
        assertThat(sender.getPassword()).isEqualTo("p");
    }

    // ── datasource ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getDataSource builds a lazy PostgreSQL Hikari pool from the properties")
    void getDataSource_buildsHikariPool() {
        HikariDataSource hikari = (HikariDataSource) config.getDataSource();
        assertThat(hikari.getJdbcUrl()).isEqualTo("jdbc:postgresql://localhost:5432/cwf");
        assertThat(hikari.getDriverClassName()).isEqualTo("org.postgresql.Driver");
        assertThat(hikari.getUsername()).isEqualTo("user");
        assertThat(hikari.getMaximumPoolSize()).isEqualTo(50);
        assertThat(hikari.isRunning()).isFalse();
    }

    // ── transactionManager ────────────────────────────────────────────────────

    @Test
    @DisplayName("transactionManager returns JpaTransactionManager bound to given EMF")
    void transactionManager_returnsJpaTransactionManager() {
        EntityManagerFactory emf = mock(EntityManagerFactory.class);
        PlatformTransactionManager tm = config.transactionManager(emf);
        assertThat(tm).isInstanceOf(JpaTransactionManager.class);
        assertThat(((JpaTransactionManager) tm).getEntityManagerFactory()).isSameAs(emf);
    }

    // ── additionalProperties ──────────────────────────────────────────────────

    @Test
    @DisplayName("additionalProperties sets hibernate.dialect to PostgreSQLDialect")
    void additionalProperties_setsPostgresDialect() {
        Properties hibernate = config.additionalProperties("TEST_SCHEMA");
        assertThat(hibernate.getProperty("hibernate.dialect"))
                .isEqualTo("org.hibernate.dialect.PostgreSQLDialect");
    }

    @Test
    @DisplayName("additionalProperties sets hibernate.default_schema and no JTA settings")
    void additionalProperties_setsDefaultSchemaWithoutJta() {
        Properties hibernate = config.additionalProperties("TEST_SCHEMA");
        assertThat(hibernate.getProperty("hibernate.default_schema")).isEqualTo("TEST_SCHEMA");
        assertThat(hibernate.containsKey("hibernate.transaction.jta.platform")).isFalse();
        assertThat(hibernate.containsKey("hibernate.transaction.coordinator_class")).isFalse();
    }

    // ── simple property-backed beans ──────────────────────────────────────────

    @Test
    @DisplayName("string beans return the bound property values")
    void stringBeans_returnValues() {
        assertThat(config.getEnvironment()).isEqualTo("LOCAL");
        assertThat(config.getSchemaName()).isEqualTo("cwf");
        assertThat(config.getFromAddress()).isEqualTo("a@b.c");
        assertThat(config.getUserName()).isEqualTo("mu");
        assertThat(config.getPassword()).isEqualTo("mp");
        assertThat(config.getPomUrl()).isEqualTo("pom");
        assertThat(config.getdealUrl()).isEqualTo("deal");
        assertThat(config.getdealAppUrl()).isEqualTo("app");
        assertThat(config.getRenewalOverviewUrl()).isEqualTo("ren");
        assertThat(config.swisscomOAuthClient()).isEqualTo("c");
        assertThat(config.swisscomOAuthSecret()).isEqualTo("s");
        assertThat(config.uwbDefaultOauth2Scope()).isEqualTo("uwb");
        assertThat(config.pomDefaultOauth2Scope()).isEqualTo("pom-scope");
        assertThat(config.graphOauth2Scope()).isEqualTo("graph");
    }
}
