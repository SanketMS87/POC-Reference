package com.rukamini.mail.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rukamini.mail.controller.dto.SendEmailRequest;
import com.rukamini.mail.exception.MailException;
import com.rukamini.mail.integration.RestService;
import com.rukamini.mail.oauth.OAuth2ClientConfig;
import com.rukamini.mail.util.MailUtils;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Generic notification orchestrator. Dispatches on
 * {@link SendEmailRequest#getTemplateName()} to the template-specific builder.
 *
 * <p>To add a new template:
 * <ol>
 *   <li>Add a constant for the template name.</li>
 *   <li>Add a private {@code sendXxxMail(SendEmailRequest)} method that
 *       fetches whatever upstream data it needs and builds the placeholder map.</li>
 *   <li>Add a new {@code case} in {@link #sendEmail(SendEmailRequest)}.</li>
 * </ol>
 *
 * <p>All templates are dispatched via
 * {@link MailBO#sendTemplateMail(String, Map, java.util.List, java.util.List)},
 * which bypasses {@code T_NOTIFICATION_PREFERENCES} ΓÇö the caller is
 * responsible for supplying the exact TO/CC recipient list.
 */
@Component
public class NotificationBO {

    /** Template for the UW Authority Self-Declaration confirmation mail. */
    public static final String TEMPLATE_UW_AUTHORITY_SELF_DECLARATION =
            "UW_AUTHORITY_SELF_DECLARATION";

    private static final String SUCCESS_KEY = "SUCCESS";

    /** Deal API returns dates as {@code yyyyMMdd} strings (e.g. {@code 20261110}). */
    private static final DateTimeFormatter DEAL_API_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    /** Human-readable format used inside rendered email bodies. */
    private static final DateTimeFormatter DISPLAY_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    @Autowired RestService restService;

    @Autowired MailBO mailBO;

    @Autowired MailUtils mailUtils;

    @Autowired ObjectMapper objectMapper;

    @Autowired private String dealUrl;

    @Autowired private String renewalOverviewUrl;

    /** Dispatches to the correct template handler. */
    public void sendEmail(SendEmailRequest request) throws MailException {
        if (request == null || request.getTemplateName() == null) {
            throw new MailException("templateName is required");
        }
        String templateName = request.getTemplateName();
        if (TEMPLATE_UW_AUTHORITY_SELF_DECLARATION.equalsIgnoreCase(templateName)) {
            sendUwAuthoritySelfDeclarationMail(request);
        } else {
            throw new MailException("Unsupported templateName: " + templateName);
        }
    }

    /**
     * Builds and sends the {@code UW_AUTHORITY_SELF_DECLARATION} mail.
     *
     * <p>Recipient: the assignee (S-prefixed user id) is resolved to an email
     * via {@code V_USERS} inside {@link MailBO}. No CC.
     */
    private void sendUwAuthoritySelfDeclarationMail(SendEmailRequest request) throws MailException {
        Map<String, Object> deal = fetchDeal(request.getDealNumber());

        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("Assignee", spaceAfterComma(safeUserName(request.getAssigneeUserId())));
        placeholders.put("DealUW", spaceAfterComma(asString(deal.get("underwriterName"))));
        placeholders.put("UnderwriterName", spaceAfterComma(asString(deal.get("underwriterName"))));
        placeholders.put("dealNumber", request.getDealNumber());
        placeholders.put("DealName", asString(deal.get("dealName")));
        placeholders.put("BusinessArea", asString(deal.get("busAreaName")));
        placeholders.put("InceptionDate", formatDealDate(asString(deal.get("inceptionDate"))));
        placeholders.put("ExpirationDate", formatDealDate(asString(deal.get("expirationDate"))));
        placeholders.put("UpdatedDate", LocalDate.now().format(DISPLAY_DATE_FORMAT));
        placeholders.put(
                "RenewalOverviewDealUrl", buildRenewalOverviewUrl(request.getDealNumber()));
        placeholders.put("DocumentName", asString(request.getDocumentName()));

        mailBO.sendTemplateMail(
                TEMPLATE_UW_AUTHORITY_SELF_DECLARATION,
                placeholders,
                Collections.singletonList(request.getAssigneeUserId()),
                Collections.emptyList());
    }

    /** Fetches the deal JSON via OAuth2 and parses it into a map. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchDeal(String dealNum) throws MailException {
        try {
            URL url = new URL(dealUrl + "api/Deals/" + dealNum);
            Map<String, String> response =
                    restService.getCallOAuth2(url.toString(), OAuth2ClientConfig.UWB_DEFAULT);
            if (response.get(SUCCESS_KEY) == null) {
                throw new MailException("Deal data retrieval failed for dealNum=" + dealNum);
            }
            return objectMapper.readValue(
                    response.get(SUCCESS_KEY), new TypeReference<HashMap<String, Object>>() {});
        } catch (MailException e) {
            throw e;
        } catch (MalformedURLException e) {
            throw new MailException("Invalid deal URL for dealNum=" + dealNum, e);
        } catch (Exception e) {
            throw new MailException("Failed to fetch deal for dealNum=" + dealNum, e);
        }
    }

    private String buildRenewalOverviewUrl(String dealNumber) {
        return (renewalOverviewUrl != null ? renewalOverviewUrl : "") + dealNumber;
    }

    private String safeUserName(String userId) {
        if (userId == null || userId.isEmpty()) {
            return "";
        }
        String name = mailUtils.getUserName(userId);
        return name != null ? name : "";
    }

    private String asString(Object value) {
        return value == null ? "" : value.toString();
    }

    // Inserts a space after every comma that isn't already followed by one (e.g. "Arani,Charan" ->
    // "Arani, Charan").
    private String spaceAfterComma(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.replaceAll(",(?!\\s)", ", ");
    }

    /**
     * Converts a Deal-API {@code yyyyMMdd} string (e.g. {@code 20261110})
     * into a display date (e.g. {@code 10-Nov-2026}). Returns an empty string
     * for null/blank input and falls back to the raw input if it does not
     * match the expected format.
     */
    private String formatDealDate(String rawDate) {
        if (rawDate == null || rawDate.isEmpty()) {
            return "";
        }
        try {
            return LocalDate.parse(rawDate, DEAL_API_DATE_FORMAT).format(DISPLAY_DATE_FORMAT);
        } catch (Exception e) {
            return rawDate;
        }
    }
}
