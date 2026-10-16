package com.rukamini.mail.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rukamini.mail.exception.MailException;
import com.rukamini.mail.integration.DealAPIModel;
import com.rukamini.mail.integration.DealServiceClient;
import com.rukamini.mail.integration.RestService;
import com.rukamini.mail.oauth.OAuth2ClientConfig;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MailHelperBO {

    /** S1192: key returned by {@link com.rukamini.mail.integration.RestService} on successful calls. */
    private static final String SUCCESS_KEY = "SUCCESS";

    private final RestService restService;

    private final DealServiceClient dealServiceClient;

    private final String pomUrl;

    private final MailBO mailBO;

    @SuppressWarnings("unchecked")
    public void sendMailNotification(String dealNum, String policyNum, String templateName)
            throws MailException {
        try {
            DealAPIModel dealModel = dealServiceClient.getDeal(dealNum);
            Map<String, String> reponse =
                    restService.getCallOAuth2(
                            pomUrl + policyNum + "?format=LEGACY", OAuth2ClientConfig.POM_DEFAULT);

            // Fail fast when the REST call returned FAILURE instead of SUCCESS
            if (reponse.get(SUCCESS_KEY) == null) {
                throw new MailException("Policy data retrieval failed for policyNum=" + policyNum);
            }

            Map<String, Object> policyMap =
                    new ObjectMapper()
                            .readValue(
                                    reponse.get(SUCCESS_KEY),
                                    new TypeReference<HashMap<String, Object>>() {});

            // Guard against upstream APIs that omit the attributes map
            Object rawAttributes = policyMap.get("attributes");
            if (rawAttributes == null) {
                throw new MailException(
                        "Policy response missing 'attributes' for policyNum=" + policyNum);
            }
            Map<String, Object> attributes = (Map<String, Object>) rawAttributes;

            String brokerName = (String) attributes.get("brokerName");

            if (StringUtils.isEmpty(brokerName)) {
                brokerName = (String) attributes.get("retailBrokerName");
            }

            if (StringUtils.isEmpty(brokerName)) {
                brokerName = " - ";
            }

            Map<String, Object> mailPolicyCreationObject = new HashMap<>();
            mailPolicyCreationObject.put("dealNumber", dealNum);
            mailPolicyCreationObject.put("templateName", templateName);
            mailPolicyCreationObject.put("insuredName", dealModel.getInsuredName());
            mailPolicyCreationObject.put("underwriterId", dealModel.getUnderwriterId());
            mailPolicyCreationObject.put("underwriterName", dealModel.getUnderwriterName());
            mailPolicyCreationObject.put("uwAssistantId", dealModel.getUwAssistantId());
            mailPolicyCreationObject.put("brokerName", brokerName);
            mailPolicyCreationObject.put("attributeId", "10580");
            mailBO.sendMailNotification(mailPolicyCreationObject);
        } catch (MailException e) {
            throw e;
        } catch (Exception e) {
            // S2139: removed LOGGER.severe — rethrow only; GlobalExceptionHandler logs it
            throw new MailException("Failed to send policy creation mail: " + e.getMessage(), e);
        }
    }

    public void sendDeclineDealOrPolicyMailNotification(String dealNum, String templateName)
            throws MailException {
        try {
            DealAPIModel dealModel = dealServiceClient.getDeal(dealNum);
            Map<String, Object> dealDeclineMailCreationObject = new HashMap<>();
            dealDeclineMailCreationObject.put("dealNumber", dealNum);
            dealDeclineMailCreationObject.put("templateName", templateName);
            dealDeclineMailCreationObject.put("insuredName", dealModel.getInsuredName());
            dealDeclineMailCreationObject.put("underwriterId", dealModel.getUnderwriterId());
            dealDeclineMailCreationObject.put("underwriterName", dealModel.getUnderwriterName());
            dealDeclineMailCreationObject.put("uwAssistantId", dealModel.getUwAssistantId());
            dealDeclineMailCreationObject.put("attributeId", "10581");
            mailBO.sendMailNotification(dealDeclineMailCreationObject);
        } catch (MailException e) {
            throw e;
        } catch (Exception e) {
            // S2139: removed LOGGER.severe — rethrow only
            throw new MailException(
                    "Failed to send deal decline mail: " + e.getMessage(),
                    e,
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }
}
