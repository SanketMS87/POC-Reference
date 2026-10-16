package com.rukamini.mail.configuration;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import java.util.Properties;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Bean definitions for the embedded Tomcat runtime (PostgreSQL {@code cwf} datasource, mail
 * sender, downstream URLs and OAuth2 credentials/scopes), driven by {@link AppProperties}.
 *
 * <p>Values come from {@code application*.properties}; secrets are resolved from Azure Key Vault
 * when enabled and from environment variables otherwise.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(AppProperties.class)
@EnableJpaRepositories(
        entityManagerFactoryRef = "cwfentityManagerFactory",
        transactionManagerRef = "cwfTransactionManager",
        basePackages = {"com.rukamini.mail.jpa.repository"})
public class AppConfig {

    private final AppProperties props;

    public AppConfig(AppProperties props) {
        this.props = props;
    }

    @Bean(name = "env")
    public String getEnvironment() {
        return props.getEnvironment();
    }

    @Bean(name = "defaultSchema")
    public String getSchemaName() {
        return props.getDefaultSchema();
    }

    /**
     * Lazily-initialised Hikari pool: the first connection is only opened on first use, so the
     * application can start while the database is unreachable (health reports DOWN instead).
     */
    @Primary
    @Bean(name = "cwfdataSource", destroyMethod = "close")
    public DataSource getDataSource() {
        AppProperties.Cwf cwf = props.getDatasource().getCwf();
        AppProperties.Pool pool = cwf.getPool();
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setPoolName("cwf");
        dataSource.setDriverClassName(cwf.getDriverClassName());
        dataSource.setJdbcUrl(cwf.getUrl());
        dataSource.setUsername(cwf.getUsername());
        dataSource.setPassword(cwf.getPassword());
        dataSource.setMinimumIdle(pool.getMinIdle());
        dataSource.setMaximumPoolSize(pool.getMaxSize());
        dataSource.setConnectionTimeout(pool.getConnectionTimeoutMs());
        dataSource.setValidationTimeout(pool.getValidationTimeoutMs());
        dataSource.setIdleTimeout(pool.getIdleTimeoutMs());
        dataSource.setMaxLifetime(pool.getMaxLifetimeMs());
        return dataSource;
    }

    @Primary
    @Bean(name = "cwfentityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            @Qualifier("cwfdataSource") DataSource dataSource,
            @Qualifier("defaultSchema") String schema) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.rukamini.mail.jpa.entity");
        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        em.setJpaProperties(additionalProperties(schema));
        em.setPersistenceUnitName("cwf");
        return em;
    }

    @Primary
    @Bean(name = "cwfTransactionManager")
    public PlatformTransactionManager transactionManager(
            @Qualifier("cwfentityManagerFactory") EntityManagerFactory emf) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(emf);
        return transactionManager;
    }

    Properties additionalProperties(String schema) {
        Properties properties = new Properties();
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.setProperty("hibernate.default_schema", schema);
        properties.setProperty("hibernate.show_sql", "true");
        properties.setProperty("hibernate.temp.use_jdbc_metadata_defaults", "false");
        return properties;
    }

    @Bean(name = "fromAddress")
    public String getFromAddress() {
        return props.getMail().getSenderAddress();
    }

    @Bean(name = "mailTechUser")
    public String getUserName() {
        return props.getMail().getTechUser();
    }

    @Bean(name = "mailTechPass")
    public String getPassword() {
        return props.getMail().getTechPass();
    }

    @Bean(name = "sender")
    public JavaMailSender getJavaMailSender(
            @Qualifier("mailTechUser") String username,
            @Qualifier("mailTechPass") String password) {
        AppProperties.Mail mail = props.getMail();
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(mail.getHost());
        mailSender.setPort(mail.getPort());
        mailSender.setUsername(username);
        mailSender.setPassword(password);
        mailSender.setJavaMailProperties(javaMailProperties(mail.getSmtp()));
        log.info(
                "AppConfig: using SMTP host='{}:{}' (auth={}, starttls={})",
                mail.getHost(),
                mail.getPort(),
                mail.getSmtp().isAuth(),
                mail.getSmtp().isStarttls());
        return mailSender;
    }

    Properties javaMailProperties(AppProperties.Smtp smtp) {
        Properties mailProps = new Properties();
        mailProps.put("mail.transport.protocol", "smtp");
        mailProps.put("mail.smtp.starttls.enable", String.valueOf(smtp.isStarttls()));
        mailProps.put("mail.smtp.auth", String.valueOf(smtp.isAuth()));
        mailProps.put("mail.debug", "false");
        if (smtp.getSslTrust() != null && !smtp.getSslTrust().isBlank()) {
            mailProps.put("mail.smtp.ssl.trust", smtp.getSslTrust());
        }
        return mailProps;
    }

    @Bean(name = "pomUrl")
    public String getPomUrl() {
        return props.getUrls().getPom();
    }

    @Bean(name = "dealUrl")
    public String getdealUrl() {
        return props.getUrls().getDeal();
    }

    @Bean(name = "dealAppUrl")
    public String getdealAppUrl() {
        return props.getUrls().getDealApp();
    }

    @Bean(name = "renewalOverviewUrl")
    public String getRenewalOverviewUrl() {
        return props.getUrls().getRenewalOverview();
    }

    @Bean("swisscomOAuthClient")
    public String swisscomOAuthClient() {
        return props.getOauth2().getSwisscomClient();
    }

    @Bean("swisscomOAuthSecret")
    public String swisscomOAuthSecret() {
        return props.getOauth2().getSwisscomSecret();
    }

    @Bean("uwbDefaultOauth2Scope")
    public String uwbDefaultOauth2Scope() {
        return props.getOauth2().getUwbScope();
    }

    @Bean("pomDefaultOauth2Scope")
    public String pomDefaultOauth2Scope() {
        return props.getOauth2().getPomScope();
    }

    @Bean("graphOauth2Scope")
    public String graphOauth2Scope() {
        return props.getOauth2().getGraphScope();
    }
}
