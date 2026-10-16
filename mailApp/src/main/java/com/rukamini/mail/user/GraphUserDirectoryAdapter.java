package com.rukamini.mail.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rukamini.mail.integration.RestService;
import com.rukamini.mail.jpa.entity.TUser;
import com.rukamini.mail.jpa.repository.TUserRepository;
import com.rukamini.mail.oauth.OAuth2ClientConfig;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * {@link UserDirectoryService} adapter backed by Microsoft Graph
 * {@code /v1.0/users}.
 *
 *
 * <h2>Lookup strategy</h2>
 * <ol>
 *   <li><b>CWF T_USER</b> — local PostgreSQL lookup by {@code USER_ID}
 *       (case-insensitive, active users only). Zero network cost; returns
 *       immediately when a match is found.
 *   <li><b>Graph primary</b> — {@code $filter=onPremisesSamAccountName eq
 *       '{id}' and accountEnabled eq true}; reached only when T_USER has no
 *       record for the given ID.
 *   <li><b>Graph fallback</b> — {@code $filter=displayName eq '{id}' and
 *       accountEnabled eq true}; supports {@code "FirstName LastName"}
 *       lookups not coverable by T_USER.
 * </ol>
 *
 * <p>Only users with {@code accountEnabled=true} are returned, mirroring the
 * IDAM {@code LIFE_CYCLE_STATE='ENABLED'} filter.
 *
 * <h2>OData escaping</h2>
 * <p>Single quotes inside filter values are doubled ({@code ''}) per the OData
 * 4.0 specification (§5.1.1.6.1) before the value is URL-encoded.
 *
 * <h2>Caching</h2>
 * <p>Results are cached in the {@code "graphUsers"} Caffeine cache
 * (10 min TTL, max 500 entries, stats enabled — see
 * {@link com.rukamini.mail.configuration.CacheConfig}).
 * {@code null} results are not cached ({@code unless="#result == null"}) so
 * that transient Graph failures are retried on the next call.
 *
 * <h2>Authentication</h2>
 * <p>Uses the {@link OAuth2ClientConfig#GRAPH_DEFAULT} client registration
 * (same {@code swisscomOAuthClient} / {@code swisscomOAuthSecret} credentials,
 * scope {@code https://graph.microsoft.com/.default}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GraphUserDirectoryAdapter implements UserDirectoryService {

    private static final String GRAPH_USERS_BASE = "https://graph.microsoft.com/v1.0/users";
    private static final String GRAPH_SELECT = "mail,givenName,surname";

    /**
     * Headers required by Microsoft Graph advanced queries (e.g. filtering on
     * {@code onPremisesSamAccountName}).  Without these, Graph returns
     * {@code 400 Request_UnsupportedQuery}.
     *
     * @see <a href="https://learn.microsoft.com/en-us/graph/aad-advanced-queries">
     *      Advanced query capabilities – Microsoft Graph</a>
     */
    private static final HttpHeaders GRAPH_ADVANCED_QUERY_HEADERS;

    static {
        GRAPH_ADVANCED_QUERY_HEADERS = new HttpHeaders();
        GRAPH_ADVANCED_QUERY_HEADERS.set("ConsistencyLevel", "eventual");
    }

    private final RestService restService;
    private final TUserRepository tUserRepository;

    /**
     * {@inheritDoc}
     *
     * <p>L1: CWF {@code T_USER} table — fast local PostgreSQL lookup.<br>
     * L2: Microsoft Graph API — reached only on T_USER miss.
     *
     * <p>Results are cached for 10 minutes (max 500 entries) as configured in
     * {@link com.rukamini.mail.configuration.CacheConfig}.
     * {@code null} returns are never cached so transient failures are
     * retried automatically.
     */
    @Override
    @Cacheable(value = "graphUsers", unless = "#result == null")
    public UserDto lookupByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        log.debug("GraphUserDirectoryAdapter: looking up userId='{}'", userId);

        // L1: CWF T_USER — fast local PostgreSQL lookup
        Optional<TUser> cwfUser = tUserRepository.findActiveByUserId(userId);
        if (cwfUser.isPresent()) {
            TUser u = cwfUser.get();
            log.debug("GraphUserDirectoryAdapter: CWF T_USER hit for userId='{}'", userId);
            return new UserDto(u.getEmailAddress(), u.getFirstName(), u.getLastName());
        }
        log.debug(
                "GraphUserDirectoryAdapter: CWF T_USER miss, falling through to Graph"
                        + " for userId='{}'",
                userId);

        // L2: Microsoft Graph — SAM account name primary + displayName fallback
        String escaped = escapeOdata(userId);
        UserDto result =
                fetchByFilter(
                        "onPremisesSamAccountName eq '" + escaped + "' and accountEnabled eq true");

        if (result == null) {
            log.debug(
                    "GraphUserDirectoryAdapter: Graph primary lookup empty,"
                            + " retrying with displayName for userId='{}'",
                    userId);
            result = fetchByFilter("displayName eq '" + escaped + "' and accountEnabled eq true");
        }

        if (result == null) {
            log.debug("GraphUserDirectoryAdapter: no enabled user found for userId='{}'", userId);
        }
        return result;
    }

    // ── private helpers ───────────────────────────────────────────────────────

    /**
     * Builds the Graph OData URL and delegates to
     * {@link RestService#getCallOAuth2(String, String, Class)}, which handles OAuth2
     * authentication and JSON deserialisation via Spring's Jackson
     * {@code HttpMessageConverter} — no manual parsing required.
     *
     * @return a {@link UserDto} or {@code null} when no match / call failed
     */
    private UserDto fetchByFilter(String odataFilter) {
        String url =
                UriComponentsBuilder.fromUriString(GRAPH_USERS_BASE)
                        .queryParam("$filter", odataFilter)
                        .queryParam("$select", GRAPH_SELECT)
                        .queryParam("$count", "true")
                        .build()
                        .encode(StandardCharsets.UTF_8)
                        .toUriString();

        Optional<GraphUserListResponse> response =
                restService.getCallOAuth2(
                        url,
                        OAuth2ClientConfig.GRAPH_DEFAULT,
                        GraphUserListResponse.class,
                        GRAPH_ADVANCED_QUERY_HEADERS);

        if (response.isEmpty()) {
            log.warn(
                    "GraphUserDirectoryAdapter: Graph API call failed for filter='{}'",
                    odataFilter);
            return null;
        }

        List<GraphUser> users = response.get().getValue();
        if (users == null || users.isEmpty()) {
            return null;
        }

        GraphUser first = users.get(0);
        return new UserDto(first.getMail(), first.getGivenName(), first.getSurname());
    }

    /**
     * Escapes a single-quote character as {@code ''} per OData 4.0
     * specification §5.1.1.6.1.
     *
     * <p>Must be applied before interpolating any user-supplied value into an
     * OData {@code $filter} string to prevent 400 errors and filter injection.
     *
     * @param value raw value to escape
     * @return OData-safe value with {@code '} replaced by {@code ''}
     */
    static String escapeOdata(String value) {
        return value.replace("'", "''");
    }

    // ── Graph response model ──────────────────────────────────────────────────

    /** Typed wrapper for the Graph {@code /users?$filter=...} response envelope. */
    @Getter
    @Setter
    static class GraphUserListResponse {
        private List<GraphUser> value;

        @JsonProperty("@odata.nextLink")
        private String nextLink;
    }

    /** Typed representation of a single Graph user entry. */
    @Getter
    @Setter
    static class GraphUser {
        private String mail;
        private String givenName;
        private String surname;
    }
}
