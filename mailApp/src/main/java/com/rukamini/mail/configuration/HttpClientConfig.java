package com.rukamini.mail.configuration;

import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Defines the application's outbound HTTP client bean.
 *
 * <p>{@code HttpClients.createSystem()} configures an Apache HttpClient 5 instance that
 * reads all JVM proxy system properties ({@code -Dhttps.proxyHost},
 * {@code -Dhttp.proxyPort}, {@code -Dhttp.nonProxyHosts}, etc.) set in
 * {@code jvm.options}, ensuring MS Graph API calls are routed through the
 * corporate proxy when required.
 */
@Configuration
public class HttpClientConfig {

    @Bean("proxyAwareRestTemplate")
    public RestTemplate restTemplateWithProxy() {
        org.apache.hc.client5.http.classic.HttpClient httpClient = HttpClients.createSystem();
        return new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
    }
}
