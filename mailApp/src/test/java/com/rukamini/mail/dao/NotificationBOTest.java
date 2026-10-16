package com.rukamini.mail.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rukamini.mail.controller.dto.SendEmailRequest;
import com.rukamini.mail.exception.MailException;
import com.rukamini.mail.integration.RestService;
import com.rukamini.mail.util.MailUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** Unit tests for {@link NotificationBO}. */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationBO")
class NotificationBOTest {

    private static final String DEAL_JSON =
            "{\"dealName\":\"Test Deal\",\"underwriterName\":\"Arani,Charan Kumar\","
                    + "\"busAreaName\":\"Property\",\"inceptionDate\":\"20261110\","
                    + "\"expirationDate\":\"20271109\"}";

    @Mock private RestService restService;
    @Mock private MailBO mailBO;
    @Mock private MailUtils mailUtils;

    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks private NotificationBO notificationBO;

    private SendEmailRequest request;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationBO, "dealUrl", "https://deal.example.com/");
        ReflectionTestUtils.setField(
                notificationBO,
                "renewalOverviewUrl",
                "https://uwbench.example.com/#/renewalOverview?dealNum=");

        request = new SendEmailRequest();
        request.setTemplateName(NotificationBO.TEMPLATE_UW_AUTHORITY_SELF_DECLARATION);
        request.setDealNumber("D12345");
        request.setAssigneeUserId("S-USER123");
        request.setDocumentName("SelfDeclaration.pdf");
    }

    private void stubDealApi(String body) {
        Map<String, String> response = new HashMap<>();
        response.put("SUCCESS", body);
        when(restService.getCallOAuth2(anyString(), anyString())).thenReturn(response);
    }

    @Test
    @DisplayName("rejects a null request")
    void testNullRequest() {
        assertThatThrownBy(() -> notificationBO.sendEmail(null))
                .isInstanceOf(MailException.class)
                .hasMessageContaining("templateName is required");
    }

    @Test
    @DisplayName("rejects a request without a templateName")
    void testMissingTemplateName() {
        request.setTemplateName(null);

        assertThatThrownBy(() -> notificationBO.sendEmail(request))
                .isInstanceOf(MailException.class)
                .hasMessageContaining("templateName is required");
    }

    @Test
    @DisplayName("rejects an unknown templateName")
    void testUnsupportedTemplateName() {
        request.setTemplateName("SOME_UNKNOWN_TEMPLATE");

        assertThatThrownBy(() -> notificationBO.sendEmail(request))
                .isInstanceOf(MailException.class)
                .hasMessageContaining("Unsupported templateName");
    }

    @Test
    @DisplayName("template name matching is case-insensitive")
    void testTemplateNameIsCaseInsensitive() throws MailException {
        stubDealApi(DEAL_JSON);
        request.setTemplateName("uw_authority_self_declaration");

        notificationBO.sendEmail(request);

        verify(mailBO)
                .sendTemplateMail(
                        eq(NotificationBO.TEMPLATE_UW_AUTHORITY_SELF_DECLARATION),
                        org.mockito.ArgumentMatchers.anyMap(),
                        org.mockito.ArgumentMatchers.anyList(),
                        org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @DisplayName("wraps a failed deal lookup in a MailException")
    void testDealFetchFailure() throws MailException {
        when(restService.getCallOAuth2(anyString(), anyString())).thenReturn(new HashMap<>());

        assertThatThrownBy(() -> notificationBO.sendEmail(request))
                .isInstanceOf(MailException.class)
                .hasMessageContaining("Deal data retrieval failed");

        verify(mailBO, never())
                .sendTemplateMail(
                        anyString(),
                        org.mockito.ArgumentMatchers.anyMap(),
                        org.mockito.ArgumentMatchers.anyList(),
                        org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @DisplayName("wraps a malformed deal URL in a MailException")
    void testMalformedDealUrl() {
        ReflectionTestUtils.setField(notificationBO, "dealUrl", "not-a-valid-url://");

        assertThatThrownBy(() -> notificationBO.sendEmail(request))
                .isInstanceOf(MailException.class)
                .hasMessageContaining("Invalid deal URL");
    }

    @Test
    @DisplayName("wraps unparseable deal JSON in a MailException")
    void testDealPayloadNotParseable() {
        stubDealApi("not-json");

        assertThatThrownBy(() -> notificationBO.sendEmail(request))
                .isInstanceOf(MailException.class)
                .hasMessageContaining("Failed to fetch deal");
    }

    @Test
    @DisplayName("sends to the assignee only, with no CC recipients")
    void testRecipientsAreAssigneeOnly() throws MailException {
        stubDealApi(DEAL_JSON);

        notificationBO.sendEmail(request);

        ArgumentCaptor<List<String>> to = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<String>> cc = ArgumentCaptor.forClass(List.class);
        verify(mailBO)
                .sendTemplateMail(
                        anyString(),
                        org.mockito.ArgumentMatchers.anyMap(),
                        to.capture(),
                        cc.capture());

        assertThat(to.getValue()).containsExactly("S-USER123");
        assertThat(cc.getValue()).isEmpty();
    }

    @Test
    @DisplayName("builds the placeholder map from the deal payload and the request")
    void testPlaceholdersArePopulated() throws MailException {
        stubDealApi(DEAL_JSON);
        when(mailUtils.getUserName("S-USER123")).thenReturn("Doe,Jane");

        notificationBO.sendEmail(request);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(mailBO)
                .sendTemplateMail(
                        anyString(),
                        captor.capture(),
                        org.mockito.ArgumentMatchers.anyList(),
                        org.mockito.ArgumentMatchers.anyList());

        Map<String, Object> placeholders = captor.getValue();
        assertThat(placeholders)
                .containsEntry("Assignee", "Doe, Jane")
                .containsEntry("UnderwriterName", "Arani, Charan Kumar")
                .containsEntry("DealUW", "Arani, Charan Kumar")
                .containsEntry("dealNumber", "D12345")
                .containsEntry("DealName", "Test Deal")
                .containsEntry("BusinessArea", "Property")
                .containsEntry("InceptionDate", "10 Nov 2026")
                .containsEntry("ExpirationDate", "09 Nov 2027")
                .containsEntry("DocumentName", "SelfDeclaration.pdf")
                .containsEntry(
                        "RenewalOverviewDealUrl",
                        "https://uwbench.example.com/#/renewalOverview?dealNum=D12345");
    }

    @Test
    @DisplayName("falls back to the raw value when a deal date is not in yyyyMMdd form")
    void testUnparseableDealDateFallsBackToRawValue() throws MailException {
        stubDealApi("{\"inceptionDate\":\"not-a-date\",\"expirationDate\":\"\"}");

        notificationBO.sendEmail(request);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(mailBO)
                .sendTemplateMail(
                        anyString(),
                        captor.capture(),
                        org.mockito.ArgumentMatchers.anyList(),
                        org.mockito.ArgumentMatchers.anyList());

        assertThat(captor.getValue())
                .containsEntry("InceptionDate", "not-a-date")
                .containsEntry("ExpirationDate", "");
    }

    @Test
    @DisplayName("missing deal attributes become empty strings rather than null")
    void testMissingDealAttributesBecomeEmptyStrings() throws MailException {
        stubDealApi("{}");
        request.setDocumentName(null);

        notificationBO.sendEmail(request);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(mailBO)
                .sendTemplateMail(
                        anyString(),
                        captor.capture(),
                        org.mockito.ArgumentMatchers.anyList(),
                        org.mockito.ArgumentMatchers.anyList());

        assertThat(captor.getValue())
                .containsEntry("DealName", "")
                .containsEntry("BusinessArea", "")
                .containsEntry("DocumentName", "");
    }

    @Test
    @DisplayName("a blank assignee id resolves to an empty Assignee placeholder")
    void testBlankAssigneeResolvesToEmptyString() throws MailException {
        stubDealApi(DEAL_JSON);
        request.setAssigneeUserId("");

        notificationBO.sendEmail(request);

        assertThat(capturePlaceholders()).containsEntry("Assignee", "");
    }

    @Test
    @DisplayName("a null assignee id resolves to an empty Assignee placeholder")
    void testNullAssigneeResolvesToEmptyString() throws MailException {
        stubDealApi(DEAL_JSON);
        request.setAssigneeUserId(null);

        notificationBO.sendEmail(request);

        assertThat(capturePlaceholders()).containsEntry("Assignee", "");
    }

    @Test
    @DisplayName("an unconfigured renewalOverviewUrl degrades to the bare deal number")
    void testNullRenewalOverviewUrl() throws MailException {
        ReflectionTestUtils.setField(notificationBO, "renewalOverviewUrl", null);
        stubDealApi(DEAL_JSON);

        notificationBO.sendEmail(request);

        assertThat(capturePlaceholders()).containsEntry("RenewalOverviewDealUrl", "D12345");
    }

    /** Runs the captor dance shared by the placeholder assertions. */
    private Map<String, Object> capturePlaceholders() throws MailException {
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(mailBO)
                .sendTemplateMail(
                        anyString(),
                        captor.capture(),
                        org.mockito.ArgumentMatchers.anyList(),
                        org.mockito.ArgumentMatchers.anyList());
        return captor.getValue();
    }
}
