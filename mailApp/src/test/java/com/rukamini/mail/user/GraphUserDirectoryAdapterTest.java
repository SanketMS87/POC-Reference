package com.rukamini.mail.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rukamini.mail.integration.RestService;
import com.rukamini.mail.jpa.entity.TUser;
import com.rukamini.mail.jpa.repository.TUserRepository;
import com.rukamini.mail.oauth.OAuth2ClientConfig;
import com.rukamini.mail.user.GraphUserDirectoryAdapter.GraphUser;
import com.rukamini.mail.user.GraphUserDirectoryAdapter.GraphUserListResponse;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

/**
 * Unit tests for {@link GraphUserDirectoryAdapter}.
 *
 * <p>Verifies the primary SAM-name lookup, {@code displayName} fallback,
 * {@code accountEnabled=false} exclusion, OData apostrophe escaping, and
 * graceful handling of API failures / empty responses.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GraphUserDirectoryAdapter")
class GraphUserDirectoryAdapterTest {

    @Mock private RestService restService;
    @Mock private TUserRepository tUserRepository;

    private GraphUserDirectoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new GraphUserDirectoryAdapter(restService, tUserRepository);
        // Lenient: not all tests call lookupByUserId (e.g. escapeOdata tests)
        lenient()
                .when(tUserRepository.findActiveByUserId(anyString()))
                .thenReturn(Optional.empty());
        lenient()
                .when(
                        restService.getCallOAuth2(
                                anyString(), anyString(), eq(GraphUserListResponse.class), any()))
                .thenReturn(Optional.empty());
    }

    // ── lookupByUserId — CWF T_USER L1 ───────────────────────────────────────

    @Test
    @DisplayName("returns UserDto from T_USER without calling Graph when CWF hit")
    void lookupByUserId_cwfHit_returnsWithoutGraphCall() {
        TUser tUser = new TUser();
        tUser.setEmailAddress("jsmith@swissre.com");
        tUser.setFirstName("John");
        tUser.setLastName("Smith");
        when(tUserRepository.findActiveByUserId("jsmith")).thenReturn(Optional.of(tUser));

        UserDto result = adapter.lookupByUserId("jsmith");

        assertThat(result).isNotNull();
        assertThat(result.email()).isEqualTo("jsmith@swissre.com");
        assertThat(result.firstName()).isEqualTo("John");
        assertThat(result.lastName()).isEqualTo("Smith");
        // Graph must NOT be called when T_USER has a hit
        verify(restService, never()).getCallOAuth2(anyString(), anyString(), any(), any());
    }

    @Test
    @DisplayName("falls through to Graph when T_USER returns empty (user not in CWF)")
    void lookupByUserId_cwfMiss_callsGraph() {
        stubGraphSuccess(singleUser("jsmith@swissre.com", "John", "Smith"));

        UserDto result = adapter.lookupByUserId("jsmith");

        assertThat(result).isNotNull();
        verify(tUserRepository).findActiveByUserId("jsmith");
        verify(restService)
                .getCallOAuth2(
                        anyString(),
                        eq(OAuth2ClientConfig.GRAPH_DEFAULT),
                        eq(GraphUserListResponse.class),
                        any(HttpHeaders.class));
    }

    // ── lookupByUserId — primary (SAM account name) ───────────────────────────

    @Test
    @DisplayName("returns UserDto from primary SAM-name lookup when user found and enabled")
    void lookupByUserId_primaryHit_returnsDto() {
        stubGraphSuccess(singleUser("jsmith@swissre.com", "John", "Smith"));

        UserDto result = adapter.lookupByUserId("jsmith");

        assertThat(result).isNotNull();
        assertThat(result.email()).isEqualTo("jsmith@swissre.com");
        assertThat(result.firstName()).isEqualTo("John");
        assertThat(result.lastName()).isEqualTo("Smith");
    }

    @Test
    @DisplayName("primary lookup fires against GRAPH_DEFAULT registration")
    void lookupByUserId_usesGraphDefaultRegistration() {
        stubGraphSuccess(singleUser("jsmith@swissre.com", "John", "Smith"));

        adapter.lookupByUserId("jsmith");

        verify(restService)
                .getCallOAuth2(
                        anyString(),
                        eq(OAuth2ClientConfig.GRAPH_DEFAULT),
                        eq(GraphUserListResponse.class),
                        any(HttpHeaders.class));
    }

    // ── lookupByUserId — displayName fallback ─────────────────────────────────

    @Test
    @DisplayName("falls back to displayName lookup when primary returns empty")
    void lookupByUserId_primaryEmpty_fallsBackToDisplayName() {
        // First call (onPremisesSamAccountName) → empty list; second (displayName) → match
        when(restService.getCallOAuth2(
                        anyString(),
                        eq(OAuth2ClientConfig.GRAPH_DEFAULT),
                        eq(GraphUserListResponse.class),
                        any()))
                .thenReturn(Optional.of(emptyResponse()))
                .thenReturn(Optional.of(singleUser("jsmith@swissre.com", "John", "Smith")));

        UserDto result = adapter.lookupByUserId("John Smith");

        assertThat(result).isNotNull();
        assertThat(result.email()).isEqualTo("jsmith@swissre.com");
    }

    @Test
    @DisplayName("returns null when both primary and fallback return empty")
    void lookupByUserId_bothEmpty_returnsNull() {
        when(restService.getCallOAuth2(
                        anyString(),
                        eq(OAuth2ClientConfig.GRAPH_DEFAULT),
                        eq(GraphUserListResponse.class),
                        any()))
                .thenReturn(Optional.of(emptyResponse()));

        assertThat(adapter.lookupByUserId("nobody")).isNull();
    }

    // ── lookupByUserId — accountEnabled in OData filter ──────────────────────

    @Test
    @DisplayName("OData filter includes 'accountEnabled eq true' — filtering done server-side")
    void lookupByUserId_filterContainsAccountEnabled() {
        stubGraphSuccess(singleUser("jsmith@swissre.com", "John", "Smith"));

        adapter.lookupByUserId("jsmith");

        verify(restService)
                .getCallOAuth2(
                        contains("accountEnabled%20eq%20true"), // URL-encoded form
                        eq(OAuth2ClientConfig.GRAPH_DEFAULT),
                        eq(GraphUserListResponse.class),
                        any(HttpHeaders.class));
    }

    @Test
    @DisplayName("returns first result — Graph guarantees only enabled users via OData filter")
    void lookupByUserId_multipleResults_returnsFirst() {
        GraphUserListResponse twoUsers = new GraphUserListResponse();
        twoUsers.setValue(
                List.of(
                        graphUser("jsmith@swissre.com", "John", "Smith"),
                        graphUser("jsmith2@swissre.com", "John", "Smith2")));
        stubGraphSuccess(twoUsers);

        UserDto result = adapter.lookupByUserId("jsmith");

        assertThat(result).isNotNull();
        assertThat(result.email()).isEqualTo("jsmith@swissre.com");
    }

    @Test
    @DisplayName(
            "Graph request includes ConsistencyLevel=eventual header and $count=true (advanced query requirements)")
    void lookupByUserId_sendsConsistencyLevelAndCountHeaders() {
        stubGraphSuccess(singleUser("jsmith@swissre.com", "John", "Smith"));

        adapter.lookupByUserId("jsmith");

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<HttpHeaders> headersCaptor = ArgumentCaptor.forClass(HttpHeaders.class);
        verify(restService)
                .getCallOAuth2(
                        urlCaptor.capture(),
                        eq(OAuth2ClientConfig.GRAPH_DEFAULT),
                        eq(GraphUserListResponse.class),
                        headersCaptor.capture());

        assertThat(urlCaptor.getValue()).contains("$count=true");
        assertThat(headersCaptor.getValue().getFirst("ConsistencyLevel")).isEqualTo("eventual");
    }

    // ── lookupByUserId — null / blank guards ──────────────────────────────────

    @Test
    @DisplayName("returns null immediately for null userId without calling REST service or T_USER")
    void lookupByUserId_nullUserId_returnsNullWithoutCall() {
        assertThat(adapter.lookupByUserId(null)).isNull();
        verify(restService, never()).getCallOAuth2(anyString(), anyString(), any(), any());
        verify(tUserRepository, never()).findActiveByUserId(anyString());
    }

    @Test
    @DisplayName("returns null immediately for blank userId without calling REST service or T_USER")
    void lookupByUserId_blankUserId_returnsNullWithoutCall() {
        assertThat(adapter.lookupByUserId("   ")).isNull();
        verify(restService, never()).getCallOAuth2(anyString(), anyString(), any(), any());
        verify(tUserRepository, never()).findActiveByUserId(anyString());
    }

    // ── lookupByUserId — API failure handling ─────────────────────────────────

    @Test
    @DisplayName("returns null when REST service returns Optional.empty (network/HTTP failure)")
    void lookupByUserId_restFailure_returnsNull() {
        when(restService.getCallOAuth2(
                        anyString(),
                        eq(OAuth2ClientConfig.GRAPH_DEFAULT),
                        eq(GraphUserListResponse.class),
                        any()))
                .thenReturn(Optional.empty());

        assertThat(adapter.lookupByUserId("jsmith")).isNull();
    }

    // ── OData escaping ────────────────────────────────────────────────────────

    @Test
    @DisplayName("escapeOdata doubles single quotes per OData 4.0 §5.1.1.6.1")
    void escapeOdata_singleQuote_doubled() {
        assertThat(GraphUserDirectoryAdapter.escapeOdata("O'Brien")).isEqualTo("O''Brien");
    }

    @Test
    @DisplayName("escapeOdata leaves strings without single quotes unchanged")
    void escapeOdata_noSingleQuote_unchanged() {
        assertThat(GraphUserDirectoryAdapter.escapeOdata("jsmith")).isEqualTo("jsmith");
    }

    @Test
    @DisplayName("escapeOdata handles multiple single quotes in one value")
    void escapeOdata_multipleQuotes_allDoubled() {
        assertThat(GraphUserDirectoryAdapter.escapeOdata("it's O'Brian's"))
                .isEqualTo("it''s O''Brian''s");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void stubGraphSuccess(GraphUserListResponse response) {
        when(restService.getCallOAuth2(
                        anyString(),
                        eq(OAuth2ClientConfig.GRAPH_DEFAULT),
                        eq(GraphUserListResponse.class),
                        any()))
                .thenReturn(Optional.of(response));
    }

    private static GraphUserListResponse singleUser(String mail, String given, String surname) {
        GraphUserListResponse r = new GraphUserListResponse();
        r.setValue(List.of(graphUser(mail, given, surname)));
        return r;
    }

    private static GraphUser graphUser(String mail, String given, String surname) {
        GraphUser u = new GraphUser();
        u.setMail(mail);
        u.setGivenName(given);
        u.setSurname(surname);
        return u;
    }

    private static GraphUserListResponse emptyResponse() {
        GraphUserListResponse r = new GraphUserListResponse();
        r.setValue(List.of());
        return r;
    }
}
