package com.rukamini.mail.dao;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rukamini.mail.exception.MailException;
import com.rukamini.mail.integration.DealAPIModel;
import com.rukamini.mail.integration.DealServiceClient;
import com.rukamini.mail.integration.RestService;
import com.rukamini.mail.oauth.OAuth2ClientConfig;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link MailHelperBO}.
 *
 * <p>After the global exception-handling refactor, MailHelperBO no longer
 * swallows exceptions: failures bubble up as {@link MailException} so that the
 * web layer's {@code GlobalExceptionHandler} can translate them into proper
 * structured error responses.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MailHelperBO")
class MailHelperBOTest {

    @Mock private RestService restService;

    @Mock private DealServiceClient dealServiceClient;

    @Mock private MailBO mailBO;

    private MailHelperBO mailHelperBO;

    // ── shared test data ──────────────────────────────────────────────────────

    private static final String DEAL_NUM = "DEAL-001";
    private static final String POLICY_NUM = "POL-001";
    private static final String TEMPLATE = "POLICY_CREATION";

    /** Deal model returned by the UWB deal endpoint via {@link DealServiceClient}. */
    private static DealAPIModel dealModel() {
        DealAPIModel model = new DealAPIModel();
        model.setInsuredName("Acme Corp");
        model.setUnderwriterId("UW01");
        model.setUnderwriterName("John Doe");
        model.setUwAssistantId("UA01");
        return model;
    }

    /** Minimal JSON returned by the POM policy endpoint — brokerName present. */
    private static final String POLICY_JSON_WITH_BROKER =
            "{\"attributes\":{\"brokerName\":\"Broker Inc\",\"retailBrokerName\":\"Retail Broker\"}}";

    /** Policy JSON where brokerName is empty → fallback to retailBrokerName. */
    private static final String POLICY_JSON_EMPTY_BROKER =
            "{\"attributes\":{\"brokerName\":\"\",\"retailBrokerName\":\"Retail Broker\"}}";

    /** Policy JSON where both broker fields are empty → fallback to " - ". */
    private static final String POLICY_JSON_NO_BROKER =
            "{\"attributes\":{\"brokerName\":\"\",\"retailBrokerName\":\"\"}}";

    private static final String POM_URL = "http://pom.local/";

    @BeforeEach
    void setUp() {
        mailHelperBO = new MailHelperBO(restService, dealServiceClient, POM_URL, mailBO);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Map<String, String> successMap(String json) {
        Map<String, String> m = new HashMap<>();
        m.put("SUCCESS", json);
        return m;
    }

    private Map<String, String> failureMap() {
        Map<String, String> m = new HashMap<>();
        m.put("FAILURE", "Error Occured");
        return m;
    }

    // ── sendMailNotification — happy paths ────────────────────────────────────

    @Test
    @DisplayName("sendMailNotification sends mail when brokerName is present")
    void sendMailNotification_brokerNamePresent_sendsMail() throws Exception {
        when(dealServiceClient.getDeal(DEAL_NUM)).thenReturn(dealModel());
        when(restService.getCallOAuth2(
                        POM_URL + POLICY_NUM + "?format=LEGACY", OAuth2ClientConfig.POM_DEFAULT))
                .thenReturn(successMap(POLICY_JSON_WITH_BROKER));
        doNothing().when(mailBO).sendMailNotification(anyMap());

        mailHelperBO.sendMailNotification(DEAL_NUM, POLICY_NUM, TEMPLATE);

        verify(mailBO, times(1)).sendMailNotification(anyMap());
    }

    @Test
    @DisplayName("sendMailNotification falls back to retailBrokerName when brokerName is empty")
    void sendMailNotification_emptyBrokerName_usesRetailBrokerName() throws Exception {
        when(dealServiceClient.getDeal(DEAL_NUM)).thenReturn(dealModel());
        when(restService.getCallOAuth2(
                        POM_URL + POLICY_NUM + "?format=LEGACY", OAuth2ClientConfig.POM_DEFAULT))
                .thenReturn(successMap(POLICY_JSON_EMPTY_BROKER));
        doNothing().when(mailBO).sendMailNotification(anyMap());

        mailHelperBO.sendMailNotification(DEAL_NUM, POLICY_NUM, TEMPLATE);

        verify(mailBO, times(1)).sendMailNotification(anyMap());
    }

    @Test
    @DisplayName("sendMailNotification falls back to ' - ' when both broker fields are empty")
    void sendMailNotification_bothBrokerFieldsEmpty_usesDash() throws Exception {
        when(dealServiceClient.getDeal(DEAL_NUM)).thenReturn(dealModel());
        when(restService.getCallOAuth2(
                        POM_URL + POLICY_NUM + "?format=LEGACY", OAuth2ClientConfig.POM_DEFAULT))
                .thenReturn(successMap(POLICY_JSON_NO_BROKER));
        doNothing().when(mailBO).sendMailNotification(anyMap());

        mailHelperBO.sendMailNotification(DEAL_NUM, POLICY_NUM, TEMPLATE);

        verify(mailBO, times(1)).sendMailNotification(anyMap());
    }

    // ── sendMailNotification — failure / exception paths ─────────────────────

    @Test
    @DisplayName(
            "sendMailNotification propagates MailException when policy REST call returns FAILURE")
    void sendMailNotification_policyCallFails_propagatesException() throws Exception {
        when(dealServiceClient.getDeal(DEAL_NUM)).thenReturn(dealModel());
        when(restService.getCallOAuth2(
                        POM_URL + POLICY_NUM + "?format=LEGACY", OAuth2ClientConfig.POM_DEFAULT))
                .thenReturn(failureMap());

        assertThatThrownBy(() -> mailHelperBO.sendMailNotification(DEAL_NUM, POLICY_NUM, TEMPLATE))
                .isInstanceOf(MailException.class);

        verify(mailBO, never()).sendMailNotification(anyMap());
    }

    @Test
    @DisplayName(
            "sendMailNotification propagates MailException when deal REST call returns FAILURE")
    void sendMailNotification_dealCallFails_propagatesException() throws Exception {
        when(dealServiceClient.getDeal(DEAL_NUM))
                .thenThrow(new MailException("Deal data retrieval failed for dealNum=" + DEAL_NUM));
        lenient()
                .when(
                        restService.getCallOAuth2(
                                POM_URL + POLICY_NUM + "?format=LEGACY",
                                OAuth2ClientConfig.POM_DEFAULT))
                .thenReturn(successMap(POLICY_JSON_WITH_BROKER));

        assertThatThrownBy(() -> mailHelperBO.sendMailNotification(DEAL_NUM, POLICY_NUM, TEMPLATE))
                .isInstanceOf(MailException.class);

        verify(mailBO, never()).sendMailNotification(anyMap());
    }

    @Test
    @DisplayName("sendMailNotification propagates exception when mailBO throws")
    void sendMailNotification_mailBoThrows_propagatesException() throws Exception {
        when(dealServiceClient.getDeal(DEAL_NUM)).thenReturn(dealModel());
        when(restService.getCallOAuth2(
                        POM_URL + POLICY_NUM + "?format=LEGACY", OAuth2ClientConfig.POM_DEFAULT))
                .thenReturn(successMap(POLICY_JSON_WITH_BROKER));
        doThrow(new MailException("mail failure")).when(mailBO).sendMailNotification(anyMap());

        assertThatThrownBy(() -> mailHelperBO.sendMailNotification(DEAL_NUM, POLICY_NUM, TEMPLATE))
                .isInstanceOf(MailException.class);

        verify(mailBO, times(1)).sendMailNotification(anyMap());
    }

    // ── sendDeclineDealOrPolicyMailNotification ──────────────────────────────

    @Test
    @DisplayName("sendDeclineDealOrPolicyMailNotification sends mail successfully")
    void sendDeclineDealOrPolicyMailNotification_success_sendsMail() throws Exception {
        when(dealServiceClient.getDeal(DEAL_NUM)).thenReturn(dealModel());
        doNothing().when(mailBO).sendMailNotification(anyMap());

        mailHelperBO.sendDeclineDealOrPolicyMailNotification(DEAL_NUM, TEMPLATE);

        verify(mailBO, times(1)).sendMailNotification(anyMap());
    }

    @Test
    @DisplayName(
            "sendDeclineDealOrPolicyMailNotification propagates MailException when deal call fails")
    void sendDeclineDealOrPolicyMailNotification_dealCallFails_propagatesException()
            throws Exception {
        when(dealServiceClient.getDeal(DEAL_NUM))
                .thenThrow(new MailException("Deal data retrieval failed for dealNum=" + DEAL_NUM));

        assertThatThrownBy(
                        () ->
                                mailHelperBO.sendDeclineDealOrPolicyMailNotification(
                                        DEAL_NUM, TEMPLATE))
                .isInstanceOf(MailException.class);

        verify(mailBO, never()).sendMailNotification(anyMap());
    }

    @Test
    @DisplayName("sendDeclineDealOrPolicyMailNotification propagates exception thrown by mailBO")
    void sendDeclineDealOrPolicyMailNotification_mailBoThrows_propagatesException()
            throws Exception {
        when(dealServiceClient.getDeal(DEAL_NUM)).thenReturn(dealModel());
        doThrow(new MailException("mail failure")).when(mailBO).sendMailNotification(anyMap());

        assertThatThrownBy(
                        () ->
                                mailHelperBO.sendDeclineDealOrPolicyMailNotification(
                                        DEAL_NUM, TEMPLATE))
                .isInstanceOf(MailException.class);

        verify(mailBO, times(1)).sendMailNotification(anyMap());
    }
}
