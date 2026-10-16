package com.rukamini.mail.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rukamini.mail.configuration.CacheConfig;
import com.rukamini.mail.integration.RestService;
import com.rukamini.mail.jpa.repository.TUserRepository;
import com.rukamini.mail.oauth.OAuth2ClientConfig;
import com.rukamini.mail.user.GraphUserDirectoryAdapter.GraphUser;
import com.rukamini.mail.user.GraphUserDirectoryAdapter.GraphUserListResponse;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Spring-context tests for {@link GraphUserDirectoryAdapter} caching behaviour.
 *
 * <p>Unit tests in {@link GraphUserDirectoryAdapterTest} call the adapter directly
 * and therefore bypass the Spring AOP proxy — {@code @Cacheable} never fires.
 * These tests load a minimal context ({@link CacheConfig} +
 * {@link GraphUserDirectoryAdapter}) so the proxy is active and cache semantics
 * can be verified end-to-end.
 */
@SpringJUnitConfig(classes = {CacheConfig.class, GraphUserDirectoryAdapter.class})
@TestPropertySource(properties = "app.user-directory.provider=graph")
@DisplayName("GraphUserDirectoryAdapter — @Cacheable")
class GraphUserDirectoryAdapterCacheTest {

    @MockitoBean private RestService restService;
    @MockitoBean private TUserRepository tUserRepository;

    @Autowired private UserDirectoryService adapter;
    @Autowired private CacheManager graphCacheManager;

    @BeforeEach
    void clearCache() {
        // Evict between tests so each starts with an empty cache
        graphCacheManager.getCache("graphUsers").clear();
        // Default: no user in T_USER — falls through to Graph
        when(tUserRepository.findActiveByUserId(anyString())).thenReturn(Optional.empty());
    }

    // ── cache hit — second call served from cache, service not called again ──

    @Test
    @DisplayName("second call with same userId returns cached result without calling Graph again")
    void lookupByUserId_cachedOnSecondCall() {
        when(restService.getCallOAuth2(
                        anyString(),
                        eq(OAuth2ClientConfig.GRAPH_DEFAULT),
                        eq(GraphUserListResponse.class),
                        any()))
                .thenReturn(Optional.of(singleUser("jsmith@swissre.com", "John", "Smith")));

        UserDto first = adapter.lookupByUserId("jsmith");
        UserDto second = adapter.lookupByUserId("jsmith");

        assertThat(first).isNotNull();
        assertThat(second).isNotNull();
        assertThat(second.email()).isEqualTo("jsmith@swissre.com");

        // Graph called exactly once — second result came from Caffeine cache
        verify(restService, times(1))
                .getCallOAuth2(
                        anyString(),
                        eq(OAuth2ClientConfig.GRAPH_DEFAULT),
                        eq(GraphUserListResponse.class),
                        any());
    }

    @Test
    @DisplayName("different userIds each trigger a separate Graph call")
    void lookupByUserId_differentKeys_callsGraphForEach() {
        when(restService.getCallOAuth2(
                        anyString(),
                        eq(OAuth2ClientConfig.GRAPH_DEFAULT),
                        eq(GraphUserListResponse.class),
                        any()))
                .thenReturn(Optional.of(singleUser("jsmith@swissre.com", "John", "Smith")));

        adapter.lookupByUserId("jsmith");
        adapter.lookupByUserId("jdoe");

        verify(restService, times(2))
                .getCallOAuth2(
                        anyString(),
                        eq(OAuth2ClientConfig.GRAPH_DEFAULT),
                        eq(GraphUserListResponse.class),
                        any());
    }

    // ── null result NOT cached — Graph retried on next call ──────────────────

    @Test
    @DisplayName("null result (user not found) is never cached — Graph retried on next call")
    void lookupByUserId_nullNotCached_graphCalledEachTime() {
        // Each lookupByUserId call fires TWO Graph requests (primary + displayName fallback)
        // when both return empty. 2 calls × 2 requests = 4 total.
        when(restService.getCallOAuth2(
                        anyString(),
                        eq(OAuth2ClientConfig.GRAPH_DEFAULT),
                        eq(GraphUserListResponse.class),
                        any()))
                .thenReturn(Optional.of(emptyResponse()));

        UserDto first = adapter.lookupByUserId("ghost");
        UserDto second = adapter.lookupByUserId("ghost");

        assertThat(first).isNull();
        assertThat(second).isNull();

        // 2 lookupByUserId calls × 2 Graph requests each = 4 total — null not cached
        verify(restService, times(4))
                .getCallOAuth2(
                        anyString(),
                        eq(OAuth2ClientConfig.GRAPH_DEFAULT),
                        eq(GraphUserListResponse.class),
                        any());
    }

    @Test
    @DisplayName("after null result, a subsequent successful lookup is cached normally")
    void lookupByUserId_firstNullThenHit_hitIsCached() {
        // Call 1: primary→empty, fallback→empty → null (not cached, 2 Graph calls)
        // Call 2: primary→hit → UserDto (cached, 1 Graph call)
        // Call 3: served from Caffeine cache (0 Graph calls)
        when(restService.getCallOAuth2(
                        anyString(),
                        eq(OAuth2ClientConfig.GRAPH_DEFAULT),
                        eq(GraphUserListResponse.class),
                        any()))
                .thenReturn(Optional.of(emptyResponse())) // call 1 primary → miss
                .thenReturn(Optional.of(emptyResponse())) // call 1 fallback → miss → null
                .thenReturn(Optional.of(singleUser("jsmith@swissre.com", "John", "Smith")));

        assertThat(adapter.lookupByUserId("jsmith")).isNull(); // miss, not cached
        assertThat(adapter.lookupByUserId("jsmith")).isNotNull(); // hit, cached
        assertThat(adapter.lookupByUserId("jsmith")).isNotNull(); // served from cache

        // 2 (call 1 null) + 1 (call 2 hit) = 3; call 3 served from cache
        verify(restService, times(3))
                .getCallOAuth2(
                        anyString(),
                        eq(OAuth2ClientConfig.GRAPH_DEFAULT),
                        eq(GraphUserListResponse.class),
                        any());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static GraphUserListResponse singleUser(String mail, String given, String surname) {
        GraphUser u = new GraphUser();
        u.setMail(mail);
        u.setGivenName(given);
        u.setSurname(surname);
        GraphUserListResponse r = new GraphUserListResponse();
        r.setValue(List.of(u));
        return r;
    }

    private static GraphUserListResponse emptyResponse() {
        GraphUserListResponse r = new GraphUserListResponse();
        r.setValue(List.of());
        return r;
    }
}
