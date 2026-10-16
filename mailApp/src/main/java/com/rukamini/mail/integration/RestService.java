package com.rukamini.mail.integration;

import com.rukamini.mail.oauth.Oauth2TokenService;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class RestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestService.class);

    private static final int CONNECT_TIMEOUT_MS = 30_000;
    private static final int SOCKET_TIMEOUT_MS = 60_000;
    private static final String CRLF_PATTERN = "[\r\n]";

    private final Oauth2TokenService oauth2TokenService;

    /**
     * Spring-managed, proxy-aware {@link RestTemplate} defined in
     * {@link com.rukamini.mail.configuration.HttpClientConfig}. Used by the typed
     * {@link #getCallOAuth2(String, String, Class)} overload — Jackson deserialisation
     * and JVM proxy settings are handled by the bean configuration.
     */
    private final RestTemplate restTemplate;

    public RestService(
            Oauth2TokenService oauth2TokenService,
            @Qualifier("proxyAwareRestTemplate") RestTemplate restTemplate) {
        this.oauth2TokenService = oauth2TokenService;
        this.restTemplate = restTemplate;
    }

    /**
     * Performs an OAuth2 Bearer-token authenticated GET request.
     *
     * <p>Uses {@link HttpClients#createSystem()} so the JVM proxy system properties
     * (-Dhttp.proxyHost / -Dhttps.proxyHost / -Dhttp.nonProxyHosts) set in JAVA_OPTS and
     * jvm.options are honoured via {@link java.net.ProxySelector#getDefault()}.
     *
     * @param url the target URL
     * @param registrationId the Spring Security client registration ID
     * @return map with key {@code "SUCCESS"} → response body, or {@code "FAILURE"} → {@code "Error
     *     Occured"} on any error
     */
    public Map<String, String> getCallOAuth2(String url, String registrationId) {
        Map<String, String> responseMap = new HashMap<>();

        RequestConfig requestConfig =
                RequestConfig.custom()
                        .setConnectTimeout(CONNECT_TIMEOUT_MS)
                        .setSocketTimeout(SOCKET_TIMEOUT_MS)
                        .build();

        try (CloseableHttpClient httpclient =
                HttpClients.custom()
                        .useSystemProperties()
                        .setDefaultRequestConfig(requestConfig)
                        .build()) {

            URI uri = new URIBuilder(url).build();
            HttpGet getReq = new HttpGet(uri);

            String token = oauth2TokenService.getAccessToken(registrationId);
            getReq.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            getReq.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());

            String safeUrl = url.replaceAll(CRLF_PATTERN, "_");
            LOGGER.info(
                    "Outbound auth applied=OAUTH2_BEARER registration='{}' url={}",
                    registrationId,
                    safeUrl);

            HttpResponse response = httpclient.execute(getReq);
            org.apache.http.HttpEntity entity = response.getEntity();
            String getResponse = (entity != null) ? EntityUtils.toString(entity) : "";

            log.info(
                    "getCallOAuth2 status={} url={}",
                    response.getStatusLine().getStatusCode(),
                    safeUrl);

            responseMap.put("SUCCESS", getResponse);
            return responseMap;
        } catch (IOException | URISyntaxException e) {
            log.info(
                    "getCallOAuth2 failed url={} error={}",
                    url.replaceAll(CRLF_PATTERN, "_"),
                    e.toString());
            responseMap.put("FAILURE", "Error Occured");
            return responseMap;
        }
    }

    /**
     * Typed OAuth2 Bearer-token authenticated GET — deserialises the JSON response body directly
     * into {@code responseType} via Spring's Jackson {@link
     * org.springframework.http.converter.json.MappingJackson2HttpMessageConverter}.
     *
     * <p>No manual JSON parsing or {@link com.fasterxml.jackson.databind.ObjectMapper} required by
     * the caller. Returns {@link Optional#empty()} on any network error, HTTP 4xx/5xx, or
     * deserialisation failure so callers can handle absence without try/catch.
     *
     * @param url the target URL (must already be fully encoded)
     * @param registrationId the Spring Security client registration ID
     * @param responseType the class to deserialise the response body into
     * @return an {@link Optional} containing the deserialised body, or empty on any failure
     */
    public <T> Optional<T> getCallOAuth2(String url, String registrationId, Class<T> responseType) {
        return getCallOAuth2(url, registrationId, responseType, null);
    }

    /**
     * Typed OAuth2 Bearer-token authenticated GET with optional extra request headers.
     *
     * <p>Extra headers (e.g. {@code ConsistencyLevel: eventual} required by Microsoft Graph
     * advanced queries) are merged on top of the standard {@code Authorization} and
     * {@code Accept} headers before the request is sent.
     *
     * @param url            the target URL (must already be fully encoded)
     * @param registrationId the Spring Security client registration ID
     * @param responseType   the class to deserialise the response body into
     * @param extraHeaders   additional headers to include, or {@code null} for none
     * @return an {@link Optional} containing the deserialised body, or empty on any failure
     */
    public <T> Optional<T> getCallOAuth2(
            String url, String registrationId, Class<T> responseType, HttpHeaders extraHeaders) {
        String token = oauth2TokenService.getAccessToken(registrationId);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        if (extraHeaders != null) {
            headers.addAll(extraHeaders);
        }

        String safeUrl = url.replaceAll(CRLF_PATTERN, "_");
        LOGGER.info(
                "Outbound auth applied=OAUTH2_BEARER registration='{}' url={}",
                registrationId,
                safeUrl);

        try {
            ResponseEntity<T> response =
                    restTemplate.exchange(
                            URI.create(url),
                            HttpMethod.GET,
                            new HttpEntity<>(headers),
                            responseType);
            LOGGER.info(
                    "getCallOAuth2 status={} url={}", response.getStatusCode().value(), safeUrl);
            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            LOGGER.warn("getCallOAuth2 failed url={} error={}", safeUrl, e.getMessage());
            return Optional.empty();
        }
    }
}
