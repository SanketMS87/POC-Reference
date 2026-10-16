package com.rukamini.mail.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rukamini.mail.exception.MailException;
import com.rukamini.mail.oauth.OAuth2ClientConfig;
import com.rukamini.mail.oauth.Oauth2TokenService;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * OAuth2-authenticated client for the Deal API {@code GET /external/api/Deals/{id}} endpoint.
 *
 * <p>Unlike {@link RestService#getCallOAuth2(String, String)}, this client does not surface the
 * raw HTTP outcome as an ad-hoc {@code SUCCESS}/{@code FAILURE} map. Instead it returns a typed
 * {@link DealAPIModel} on success (2xx) and throws a {@link MailException} for any non-2xx
 * response or transport-level failure.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DealServiceClient {

    private static final int CONNECT_TIMEOUT_MS = 30_000;
    private static final int SOCKET_TIMEOUT_MS = 60_000;

    private final Oauth2TokenService oauth2TokenService;

    private final String dealUrl;

    /**
     * Fetches the deal identified by {@code dealNum} from the Deal API.
     *
     * @param dealNum the deal (or policy) number
     * @return the deserialized {@link DealAPIModel}
     * @throws MailException if the deal URL is invalid, the request fails, or the response status
     *     is not 2xx
     */
    public DealAPIModel getDeal(String dealNum) throws MailException {
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

            String url = buildUrl(dealNum);
            URI uri = new URIBuilder(url).build();
            HttpGet getReq = new HttpGet(uri);

            String token = oauth2TokenService.getAccessToken(OAuth2ClientConfig.UWB_DEFAULT);
            getReq.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            getReq.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());

            String safeUrl = url.replaceAll("[\r\n]", "_");
            log.info(
                    "Outbound auth applied=OAUTH2_BEARER registration='{}' url={}",
                    OAuth2ClientConfig.UWB_DEFAULT,
                    safeUrl);

            HttpResponse response = httpclient.execute(getReq);
            HttpEntity entity = response.getEntity();
            String body = (entity != null) ? EntityUtils.toString(entity) : "";
            int statusCode = response.getStatusLine().getStatusCode();

            log.info("getDeal status={} url={}", statusCode, safeUrl);

            if (statusCode < 200 || statusCode >= 300) {
                throw new MailException(
                        "Deal data retrieval failed for dealNum="
                                + dealNum
                                + " with status="
                                + statusCode,
                        HttpStatus.valueOf(statusCode));
            }

            return new ObjectMapper().readValue(body, DealAPIModel.class);
        } catch (IllegalArgumentException e) {
            throw new MailException("Invalid deal URL for dealNum=" + dealNum, e);
        } catch (IOException | URISyntaxException e) {
            throw new MailException("Failed to fetch deal attributes for dealNum=" + dealNum, e);
        }
    }

    private String buildUrl(String dealNum) {
        return UriComponentsBuilder.fromUriString(dealUrl)
                .pathSegment("api", "Deals", dealNum)
                .build()
                .encode()
                .toUriString();
    }
}
