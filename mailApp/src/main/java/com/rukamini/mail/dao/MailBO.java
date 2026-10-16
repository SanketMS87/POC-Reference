package com.rukamini.mail.dao;

import com.rukamini.mail.exception.MailException;
import com.rukamini.mail.jpa.entity.TTemplate;
import com.rukamini.mail.jpa.repository.NotificationPreferencesRepository;
import com.rukamini.mail.jpa.repository.TTemplateRepository;
import com.rukamini.mail.user.UserDirectoryService;
import com.rukamini.mail.user.UserDto;
import com.rukamini.mail.util.MailConstants;
import com.rukamini.mail.util.MailUtils;
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMessage.RecipientType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MailBO {

    final TTemplateRepository templateRepository;

    private final String dealAppUrl;

    private final JavaMailSender sender;

    final MailUtils mailUtils;

    private final String fromAddress;

    final UserDirectoryService userDirectoryService;

    final NotificationPreferencesRepository notificationPreferencesRepository;

    /** S1192: constant for the map key used in sendMailNotification to avoid literal duplication. */
    private static final String KEY_UNDERWRITER_ID = "underwriterId";

    private static final String KEY_UW_ASSISTANT_ID = "uwAssistantId";
    private static final String KEY_BROKER_NAME = "brokerName";
    private static final String KEY_INSURED_NAME = "insuredName";

    public MailBO(
            TTemplateRepository templateRepository,
            String dealAppUrl,
            JavaMailSender sender,
            MailUtils mailUtils,
            String fromAddress,
            UserDirectoryService userDirectoryService,
            NotificationPreferencesRepository notificationPreferencesRepository) {
        this.templateRepository = templateRepository;
        this.dealAppUrl = dealAppUrl;
        this.sender = sender;
        this.mailUtils = mailUtils;
        this.fromAddress = fromAddress;
        this.userDirectoryService = userDirectoryService;
        this.notificationPreferencesRepository = notificationPreferencesRepository;
    }

    public void sendMailNotification(Map<String, Object> mailCreationObject) throws MailException {
        try {
            log.info("sendMailNotification start"); // S5145: replaced user-data log with static
            // message

            String templateName = (String) mailCreationObject.get("templateName");
            TTemplate template = templateRepository.getMailTemplate(templateName);

            String receipents =
                    checkMailNotificationPreferencesForUser(
                            mailCreationObject.get(KEY_UNDERWRITER_ID).toString(),
                            mailCreationObject.get("attributeId").toString());
            String ccReceipents =
                    checkMailNotificationPreferencesForUser( // S117: renamed cc_receipents →
                            // ccReceipents
                            mailCreationObject.get(KEY_UW_ASSISTANT_ID).toString(),
                            mailCreationObject.get("attributeId").toString());

            Map<String, Object> mailAttributes = new HashMap<>();
            mailAttributes.put("UW_NAME", mailCreationObject.get("underwriterName").toString());
            mailAttributes.put(
                    MailConstants.DEAL_URL,
                    dealAppUrl + mailCreationObject.get("dealNumber").toString());
            mailAttributes.put(
                    MailConstants.DEAL_NUM, mailCreationObject.get("dealNumber").toString());
            mailAttributes.put(
                    "INSURED_NAME",
                    mailCreationObject.get(KEY_INSURED_NAME) == null
                            ? ""
                            : String.valueOf(mailCreationObject.get(KEY_INSURED_NAME)));
            mailAttributes.put(
                    "BROKER_NAME",
                    mailCreationObject.get(KEY_BROKER_NAME) == null
                            ? ""
                            : String.valueOf(mailCreationObject.get(KEY_BROKER_NAME)));
            mailAttributes.put(
                    MailConstants.RECEIPENTS,
                    Collections.singletonList(
                            mailCreationObject.get(KEY_UNDERWRITER_ID).toString()));
            mailAttributes.put(
                    MailConstants.CC_RECEIPENTS,
                    Collections.singletonList(
                            mailCreationObject.get(KEY_UW_ASSISTANT_ID).toString()));

            sendMail(
                    template,
                    mailAttributes,
                    mailCreationObject.get(KEY_UNDERWRITER_ID).toString(),
                    receipents,
                    ccReceipents);

        } catch (MailException e) {
            throw e;
        } catch (Exception e) {
            // S2139: removed log.log — exception propagated with context; GlobalExceptionHandler
            // logs it
            throw new MailException(
                    "Error occurred while sending notification mail: " + e.getMessage(), e);
        }
    }

    public String checkMailNotificationPreferencesForUser(
            String userId, String collaborationAttributeId) {
        log.info("checkMailNotificationPreferencesForUser start"); // S5145: removed userId from log
        String recepient = null;
        if (null != userId && !userId.isEmpty() && null != collaborationAttributeId) {
            recepient =
                    notificationPreferencesRepository.fetchUserToSendMails(
                            userId.toUpperCase(), collaborationAttributeId);
        }
        log.info("checkMailNotificationPreferencesForUser end"); // S5145: removed recepient value
        // from log
        return recepient;
    }

    /**
     * Sends a mail rendered from the given template using caller-supplied
     * recipients — bypasses the {@code T_NOTIFICATION_PREFERENCES} opt-in
     * check performed by {@link #sendMailNotification(Map)}.
     *
     * <p>Intended for generic notifications where the caller already knows
     * exactly who should receive the mail (e.g. self-declaration confirmation
     * sent to a specific assignee). User ids in {@code toUserIds}/{@code ccUserIds}
     * are resolved to email addresses via {@code V_USERS} inside
     * {@link #fetchAddress}.
     *
     * @param templateName  {@code T_TEMPLATE.TEMPLATE_NAME} to render
     * @param placeholders  variable substitutions for subject/body
     *                      ({@code ${key}} → value); RECEIPENTS/CC_RECEIPENTS
     *                      keys are overwritten by this method
     * @param toUserIds     TO recipients (user ids or direct @swissre.com addresses)
     * @param ccUserIds     CC recipients (may be empty or {@code null})
     */
    public void sendTemplateMail(
            String templateName,
            Map<String, Object> placeholders,
            List<String> toUserIds,
            List<String> ccUserIds)
            throws MailException {
        try {
            TTemplate template = templateRepository.getMailTemplate(templateName);
            if (template == null) {
                throw new MailException("Mail template not found: " + templateName);
            }
            if (placeholders == null) {
                placeholders = new HashMap<>();
            }
            List<String> to = toUserIds != null ? toUserIds : Collections.emptyList();
            List<String> cc = ccUserIds != null ? ccUserIds : Collections.emptyList();
            placeholders.put(MailConstants.RECEIPENTS, to);
            placeholders.put(MailConstants.CC_RECEIPENTS, cc);

            // sendMail() expects a non-empty String flag in the receipents/ccReceipents
            // slots to enable the TO/CC branches; it also uses the primary user id to
            // look up the ReplyTo address. Reuse the first TO user for that purpose.
            String primaryUserId = to.isEmpty() ? null : to.get(0);
            String receipentsFlag = to.isEmpty() ? null : primaryUserId;
            String ccFlag = cc.isEmpty() ? null : "cc";
            sendMail(template, placeholders, primaryUserId, receipentsFlag, ccFlag);
        } catch (MailException e) {
            throw e;
        } catch (Exception e) {
            throw new MailException(
                    "Error sending templated mail '" + templateName + "': " + e.getMessage(), e);
        }
    }

    /**
     * Builds the mail attributes, creates and configures the MimeMessage, then sends it
     * if at least one recipient is present.
     *
     * <p>Extracted {@link #buildMessage} and {@link #sendIfRecipientPresent} reduce the
     * cognitive complexity of this method to stay within the S3776 limit of 15.
     */
    public void sendMail(
            TTemplate template,
            Map<String, Object> templateData,
            String userId,
            String receipents,
            String ccReceipents)
            throws MailException { // S117: renamed cc_receipents → ccReceipents
        if (templateData == null) {
            return; // S3776: guard clause removes outer if-block, reducing nesting depth
        }
        log.info("SendMail start");
        Map<String, String> mailAttributes = new HashMap<>();
        setTemplateValues(template, templateData, mailAttributes, userId);
        try {
            MimeMessage message =
                    buildMessage(templateData, mailAttributes, receipents, ccReceipents);
            sendIfRecipientPresent(message);
        } catch (AddressException e) {
            // S2139: removed log.logp — rethrow only; GlobalExceptionHandler logs at ERROR
            throw new MailException(
                    "Exception while fetching the mail Address: " + e.getMessage(), e);
        } catch (Exception ex) {
            // S2139: removed log.logp
            throw new MailException("Exception while sending the mail: " + ex.getMessage(), ex);
        }
        log.info("SendMail end");
    }

    /**
     * Creates and fully configures a {@link MimeMessage}.
     *
     * <p>Sets the HTML body directly on the message via
     * {@code message.setContent(html, contentType)} instead of wrapping it in a
     * {@code MimeMultipart}. A single-part HTML email does not need a multipart
     * envelope, and avoiding it sidesteps the
     * {@code "no object DCH for MIME type multipart/mixed"} error that can occur when the
     * classloader cannot discover Angus Mail's {@code DataContentHandler} implementations.
     *
     * <p>Every other call ({@code setFrom}, {@code setSubject}, {@code setRecipients},
     * {@code setReplyTo}) is unchanged from the original implementation.
     */
    private MimeMessage buildMessage(
            Map<String, Object> templateData,
            Map<String, String> mailAttributes,
            String receipents,
            String ccReceipents)
            throws MessagingException {
        MimeMessage message = sender.createMimeMessage();
        message.setContent(
                mailAttributes.get(MailConstants.CONTENT), MailConstants.EMAIL_CONTENT_TYPE);
        message.setFrom(new InternetAddress(fromAddress));
        if (receipents != null && !receipents.isEmpty()) {
            UserDto replyUser = fetchVUserById(receipents);
            if (replyUser != null) {
                message.setReplyTo(InternetAddress.parse(replyUser.email()));
            }
            message.setRecipients(
                    RecipientType.TO,
                    fetchAddress(
                            templateData,
                            MailConstants.RECEIPENTS,
                            MailConstants.RECEIVER_GROUP_MAIL));
        }
        if (ccReceipents != null && !ccReceipents.isEmpty()) {
            message.setRecipients(
                    RecipientType.CC,
                    fetchAddress(
                            templateData,
                            MailConstants.CC_RECEIPENTS,
                            MailConstants.CC_RECEPIENT_GROUP));
        }
        message.setSubject(mailAttributes.get(MailConstants.SUBJECT));
        return message;
    }

    /** Sends the message only when at least one resolved TO or CC address is present. */
    private void sendIfRecipientPresent(MimeMessage message) throws MessagingException {
        Address[] to = message.getRecipients(Message.RecipientType.TO);
        Address[] cc = message.getRecipients(Message.RecipientType.CC);
        boolean hasTo = to != null && to.length > 0;
        boolean hasCc = cc != null && cc.length > 0;
        if (hasTo || hasCc) {
            sender.send(message);
        } else {
            log.info("No recipients configured — notification suppressed");
        }
    }

    /**
     * Resolves a {@link UserDto} by user ID or full name via
     * {@link UserDirectoryService}.
     *
     * <p>Delegates to the active {@link UserDirectoryService} implementation —
     * either {@link com.rukamini.mail.user.IdamUserDirectoryAdapter} (IDAM
     * Oracle DB, default) or
     * {@link com.rukamini.mail.user.GraphUserDirectoryAdapter} (Microsoft
     * Graph), selected by {@code app.user-directory.provider}.
     */
    public UserDto fetchVUserById(String userId) {
        return userDirectoryService.lookupByUserId(userId);
    }

    public void setTemplateValues(
            TTemplate template,
            Map<String, Object> templateData,
            Map<String, String> mailAttributes,
            String userId) {
        String mailBody = (template != null) ? template.getTemplateBody() : null;
        String mailSubject = (template != null) ? template.getTemplateSubject() : null;

        // S3776: loop extracted to substituteVariables() to keep this method's complexity ≤15
        mailBody = substituteVariables(mailBody, templateData);
        mailSubject = substituteVariables(mailSubject, templateData);

        if (mailSubject != null && userId != null) {
            String userName = mailUtils.getUserName(userId);
            mailSubject =
                    mailSubject.replace(
                            "${FromValue}",
                            "From :".concat(userName != null ? userName : "").concat(" "));
        } else if (mailSubject != null) {
            mailSubject = mailSubject.replace("${FromValue}", "From :".concat("CorFlow Admin :"));
        }
        if (mailSubject != null && mailSubject.contains("-  -")) {
            mailSubject = mailSubject.replace("-  -", "-");
        }
        mailAttributes.put(MailConstants.CONTENT, mailBody);
        mailAttributes.put(MailConstants.SUBJECT, mailSubject);
    }

    /**
     * Replaces every {@code ${key}} placeholder in {@code text} with the matching value
     * from {@code templateData}. Returns {@code null} if {@code text} is {@code null}.
     *
     * <p>Extracted from {@link #setTemplateValues} to reduce its cognitive complexity (S3776).
     */
    private String substituteVariables(String text, Map<String, Object> templateData) {
        if (text == null) {
            return null;
        }
        for (Map.Entry<String, Object> entry : templateData.entrySet()) {
            if (entry.getValue() != null) {
                text = text.replace("${" + entry.getKey() + "}", entry.getValue().toString());
            }
        }
        return text;
    }

    @SuppressWarnings("unchecked")
    public InternetAddress[] fetchAddress(
            Map<String, Object> templateData, String receiverType, String groupMail)
            throws AddressException {
        // S3776: both original branches resolved to the same receiverType list; collapsed here.
        if (templateData.get(groupMail) == null && templateData.get(receiverType) == null) {
            return new InternetAddress[0];
        }
        List<InternetAddress> toAddress = new ArrayList<>();
        List<String> users = (List<String>) templateData.get(receiverType);
        if (users != null) {
            addAddressesForReceivers(users, toAddress);
        }
        return toAddress.toArray(new InternetAddress[0]);
    }

    /**
     * Adds a resolved {@link InternetAddress} for each entity in {@code users} to
     * {@code toAddress}. Direct e-mail addresses are used as-is; user IDs are resolved
     * via {@link #fetchVUserById}.
     *
     * <p>Extracted from {@link #fetchAddress} to reduce its cognitive complexity (S3776).
     */
    private void addAddressesForReceivers(List<String> users, List<InternetAddress> toAddress)
            throws AddressException {
        for (String entity : users) {
            if (entity.contains("@swissre.com")) {
                toAddress.add(new InternetAddress(entity));
            } else if (!entity.isEmpty()) {
                UserDto user = fetchVUserById(entity);
                if (user != null) {
                    toAddress.add(new InternetAddress(user.email()));
                }
            }
        }
    }
}
