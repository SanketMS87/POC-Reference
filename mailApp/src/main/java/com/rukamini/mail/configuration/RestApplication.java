package com.rukamini.mail.configuration;

import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Application entry point (executable Spring Boot jar with embedded Tomcat).
 *
 * <p>All environment-specific configuration (datasource, mail, downstream URLs, OAuth2) is
 * property-backed and defined in {@link AppConfig}. This class stays
 * profile-agnostic so that component scanning, transaction management, and async support are
 * always configured regardless of which profile is active.
 */
@SpringBootApplication(exclude = {OAuth2ClientAutoConfiguration.class})
@EnableTransactionManagement
@Configuration
@EnableAsync
@ComponentScan(basePackages = {"com.rukamini.mail"})
public class RestApplication {

    public static void main(final String... args) {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        SpringApplication.run(RestApplication.class, args);
    }

    @Bean
    public PersistenceExceptionTranslationPostProcessor exceptionTranslation() {
        return new PersistenceExceptionTranslationPostProcessor();
    }
}
