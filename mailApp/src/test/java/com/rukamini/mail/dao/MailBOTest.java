package com.rukamini.mail.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.rukamini.mail.exception.MailException;
import com.rukamini.mail.jpa.entity.TTemplate;
import com.rukamini.mail.jpa.repository.NotificationPreferencesRepository;
import com.rukamini.mail.jpa.repository.TTemplateRepository;
import com.rukamini.mail.user.UserDirectoryService;
import com.rukamini.mail.user.UserDto;
import com.rukamini.mail.util.MailConstants;
import com.rukamini.mail.util.MailUtils;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Unit tests for {@link MailBO}.
 *
 * <p>All Spring beans are supplied via the constructor. The private helpers
 * {@code substituteVariables} and {@code addAddressesForReceivers} are
 * exercised indirectly through their public callers ({@code setTemplateValues}
 * and {@code fetchAddress} respectively).
 *
 * <p>{@link UserDirectoryService} is mocked so both the IDAM and Graph
 * adapters are transparent to these tests — behaviour is adapter-agnostic.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MailBO")
class MailBOTest {

    @Mock private TTemplateRepository templateRepository;
    @Mock private NotificationPreferencesRepository notificationPreferencesRepository;
    @Mock private UserDirectoryService userDirectoryService;
    @Mock private MailUtils mailUtils;
    @Mock private JavaMailSender sender;

    private MailBO mailBO;

    @BeforeEach
    void setUp() {
        mailBO =
                new MailBO(
                        templateRepository,
                        "http://deal.local/",
                        sender,
                        mailUtils,
                        "noreply@swissre.com",
                        userDirectoryService,
                        notificationPreferencesRepository);
    }

    // ── checkMailNotificationPreferencesForUser ───────────────────────────────

    @Test
    @DisplayName("returns repository result when userId and attributeId are provided")
    void checkNotifPrefs_validInput_returnsRepositoryResult() {
        when(notificationPreferencesRepository.fetchUserToSendMails("UW01", "10580"))
                .thenReturn("uw01@swissre.com");

        String result = mailBO.checkMailNotificationPreferencesForUser("uw01", "10580");

        assertThat(result).isEqualTo("uw01@swissre.com");
        verify(notificationPreferencesRepository).fetchUserToSendMails("UW01", "10580");
    }

    // S5976: replaced 3 near-identical tests with a single @ParameterizedTest
    @ParameterizedTest(name = "[{index}] userId=''{0}'' attributeId=''{1}''")
    @MethodSource("invalidPreferenceInputs")
    @DisplayName("returns null without querying for blank/null userId or null attributeId")
    void checkNotifPrefs_invalidInput_returnsNull(String userId, String attributeId) {
        assertThat(mailBO.checkMailNotificationPreferencesForUser(userId, attributeId)).isNull();
        verifyNoInteractions(notificationPreferencesRepository);
    }

    static Stream<Arguments> invalidPreferenceInputs() {
        return Stream.of(
                Arguments.of("", "10580"), // empty userId
                Arguments.of(null, "10580"), // null userId
                Arguments.of("uw01", null) // null attributeId
                );
    }

    // ── fetchVUserById ────────────────────────────────────────────────────────

    @Test
    @DisplayName("returns UserDto when the directory service finds the user")
    void fetchVUserById_found_returnsUserDto() {
        UserDto dto = new UserDto("jsmith@swissre.com", "John", "Smith");
        when(userDirectoryService.lookupByUserId("jsmith")).thenReturn(dto);

        UserDto result = mailBO.fetchVUserById("jsmith");

        assertThat(result).isNotNull();
        assertThat(result.email()).isEqualTo("jsmith@swissre.com");
        verify(userDirectoryService).lookupByUserId("jsmith");
    }

    @Test
    @DisplayName("returns null when the directory service finds no user")
    void fetchVUserById_notFound_returnsNull() {
        when(userDirectoryService.lookupByUserId("unknown")).thenReturn(null);

        assertThat(mailBO.fetchVUserById("unknown")).isNull();
    }

    // ── setTemplateValues ─────────────────────────────────────────────────────

    @Test
    @DisplayName("null template → content and subject are both null")
    void setTemplateValues_nullTemplate_setsNullContentAndSubject() {
        Map<String, String> attrs = new HashMap<>();

        mailBO.setTemplateValues(null, new HashMap<>(), attrs, "uw01");

        assertThat(attrs.get(MailConstants.CONTENT)).isNull();
        assertThat(attrs.get(MailConstants.SUBJECT)).isNull();
    }

    @Test
    @DisplayName(
            "substitutes placeholders and replaces ${FromValue} with user name when userId is given")
    void setTemplateValues_withUserId_substitutesPlaceholders() {
        TTemplate template = new TTemplate();
        template.setTemplateBody("Policy ${DEAL_NUM} for ${INSURED}");
        template.setTemplateSubject("Deal ${DEAL_NUM} - ${FromValue}");

        when(mailUtils.getUserName("uw01")).thenReturn("John Doe");

        Map<String, Object> data = new HashMap<>();
        data.put("DEAL_NUM", "D-001");
        data.put("INSURED", "Acme Corp");
        Map<String, String> attrs = new HashMap<>();

        mailBO.setTemplateValues(template, data, attrs, "uw01");

        assertThat(attrs)
                .containsEntry(MailConstants.CONTENT, "Policy D-001 for Acme Corp")
                .containsEntry(MailConstants.SUBJECT, "Deal D-001 - From :John Doe ");
    }

    @Test
    @DisplayName("replaces ${FromValue} with 'CorFlow Admin' when userId is null")
    void setTemplateValues_nullUserId_usesAdminPrefix() {
        TTemplate template = new TTemplate();
        template.setTemplateBody("Body");
        template.setTemplateSubject("Subject ${FromValue}");
        Map<String, String> attrs = new HashMap<>();

        mailBO.setTemplateValues(template, new HashMap<>(), attrs, null);

        assertThat(attrs)
                .containsEntry(MailConstants.SUBJECT, "Subject From :CorFlow Admin :"); // S5838
    }

    @Test
    @DisplayName("collapses '-  -' to '-' in the subject line")
    void setTemplateValues_doubleDashInSubject_collapsed() {
        TTemplate template = new TTemplate();
        template.setTemplateBody("Body");
        template.setTemplateSubject("Deal -  - Renewal ${FromValue}");
        Map<String, String> attrs = new HashMap<>();

        mailBO.setTemplateValues(template, new HashMap<>(), attrs, null);

        assertThat(attrs)
                .containsEntry(
                        MailConstants.SUBJECT, "Deal - Renewal From :CorFlow Admin :"); // S5838
    }

    @Test
    @DisplayName("skips substitution for placeholder keys whose value is null")
    void setTemplateValues_nullValueInData_keepsPlaceholder() {
        TTemplate template = new TTemplate();
        template.setTemplateBody("Hello ${NAME}");
        template.setTemplateSubject("Hi ${NAME} ${FromValue}");

        Map<String, Object> data = new HashMap<>();
        data.put("NAME", null);

        Map<String, String> attrs = new HashMap<>();

        mailBO.setTemplateValues(template, data, attrs, null);

        assertThat(attrs).containsEntry(MailConstants.CONTENT, "Hello ${NAME}"); // S5838
    }

    // ── fetchAddress ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("returns empty array when both groupMail and receiverType keys are absent")
    void fetchAddress_bothKeysAbsent_returnsEmptyArray() throws Exception {
        InternetAddress[] result =
                mailBO.fetchAddress(
                        new HashMap<>(),
                        MailConstants.RECEIPENTS,
                        MailConstants.RECEIVER_GROUP_MAIL);

        assertThat(result).isEmpty();
        verifyNoInteractions(userDirectoryService);
    }

    @Test
    @DisplayName("resolves @swissre.com e-mail strings directly without a directory lookup")
    void fetchAddress_emailAddresses_resolvedDirectly() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put(MailConstants.RECEIPENTS, List.of("alice@swissre.com", "bob@swissre.com"));

        InternetAddress[] result =
                mailBO.fetchAddress(
                        data, MailConstants.RECEIPENTS, MailConstants.RECEIVER_GROUP_MAIL);

        assertThat(result).hasSize(2);
        assertThat(result[0].getAddress()).isEqualTo("alice@swissre.com");
        assertThat(result[1].getAddress()).isEqualTo("bob@swissre.com");
        verifyNoInteractions(userDirectoryService);
    }

    @Test
    @DisplayName("resolves user IDs via UserDirectoryService and uses the e-mail address found")
    void fetchAddress_userIds_resolvedViaDirectoryService() throws Exception {
        UserDto dto = new UserDto("jsmith@swissre.com", "John", "Smith");
        when(userDirectoryService.lookupByUserId("jsmith")).thenReturn(dto);

        Map<String, Object> data = new HashMap<>();
        data.put(MailConstants.RECEIPENTS, List.of("jsmith"));

        InternetAddress[] result =
                mailBO.fetchAddress(
                        data, MailConstants.RECEIPENTS, MailConstants.RECEIVER_GROUP_MAIL);

        assertThat(result).hasSize(1);
        assertThat(result[0].getAddress()).isEqualTo("jsmith@swissre.com");
    }

    @Test
    @DisplayName("skips user ID entry when directory service returns null")
    void fetchAddress_userIdNotFound_skipsEntry() throws Exception {
        when(userDirectoryService.lookupByUserId("ghost")).thenReturn(null);

        Map<String, Object> data = new HashMap<>();
        data.put(MailConstants.RECEIPENTS, List.of("ghost"));

        InternetAddress[] result =
                mailBO.fetchAddress(
                        data, MailConstants.RECEIPENTS, MailConstants.RECEIVER_GROUP_MAIL);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("skips empty entity strings without a directory lookup")
    void fetchAddress_emptyEntity_skipped() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put(MailConstants.RECEIPENTS, List.of(""));

        InternetAddress[] result =
                mailBO.fetchAddress(
                        data, MailConstants.RECEIPENTS, MailConstants.RECEIVER_GROUP_MAIL);

        assertThat(result).isEmpty();
        verifyNoInteractions(userDirectoryService);
    }

    @Test
    @DisplayName("uses receiverType list when groupMail key is present in templateData")
    void fetchAddress_groupMailKeyPresent_usesReceiverTypeList() throws Exception {
        Map<String, Object> data = new HashMap<>();
        // groupMail key present → both branches in original code resolved to receiverType list
        data.put(MailConstants.RECEIVER_GROUP_MAIL, "group@swissre.com");
        data.put(MailConstants.RECEIPENTS, List.of("user@swissre.com"));

        InternetAddress[] result =
                mailBO.fetchAddress(
                        data, MailConstants.RECEIPENTS, MailConstants.RECEIVER_GROUP_MAIL);

        assertThat(result).hasSize(1);
        assertThat(result[0].getAddress()).isEqualTo("user@swissre.com");
    }

    // ── sendMail ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName(
            "sendMail with null templateData returns immediately without contacting the mail server")
    void sendMail_nullTemplateData_returnsImmediately() throws MailException {
        mailBO.sendMail(new TTemplate(), null, "uw01", null, null);

        verifyNoInteractions(sender);
    }

    @Test
    @DisplayName("sendMail with no recipients builds and suppresses the message without sending")
    void sendMail_noRecipients_suppressesSend() throws Exception {
        TTemplate template = new TTemplate();
        template.setTemplateBody("Body text");
        template.setTemplateSubject("Subject line");

        MimeMessage mockMsg = mock(MimeMessage.class);
        when(sender.createMimeMessage()).thenReturn(mockMsg);
        // Both recipient types return null → sendIfRecipientPresent must NOT call sender.send
        when(mockMsg.getRecipients(Message.RecipientType.TO)).thenReturn(null);
        when(mockMsg.getRecipients(Message.RecipientType.CC)).thenReturn(null);

        Map<String, Object> data = new HashMap<>();
        data.put(MailConstants.RECEIPENTS, Collections.emptyList());
        data.put(MailConstants.CC_RECEIPENTS, Collections.emptyList());
        data.put(MailConstants.RECEIVER_GROUP_MAIL, null);
        data.put(MailConstants.CC_RECEPIENT_GROUP, null);

        mailBO.sendMail(template, data, null, null, null);

        verify(sender, never()).send(any(MimeMessage.class));
    }

    // ── sendMail — DCH / MIME serialization ─────────────────────────────────

    @Test
    @DisplayName("sendMail produces a message whose MIME content can be serialized (no DCH error)")
    void sendMail_realMimeMessage_serializesWithoutDchError() throws Exception {
        /*
         * This test catches the exact failure from production:
         *   "no object DCH for MIME type multipart/mixed"
         *
         * It uses a REAL MimeMessage (not mocked) so that writeTo() triggers
         * the same Jakarta Activation DataContentHandler resolution that
         * JavaMailSenderImpl.doSend() does.  If buildMessage still used
         * MimeMultipart, writeTo() would throw UnsupportedDataTypeException
         * in any ClassLoader-isolated environment.
         */
        TTemplate template = new TTemplate();
        template.setTemplateBody("<p>Hello ${UW_NAME}</p>");
        template.setTemplateSubject("Deal ${DEAL_NUM} ${FromValue}");

        // Return a REAL MimeMessage backed by a dummy Session
        Session session = Session.getInstance(new Properties());
        MimeMessage realMessage = new MimeMessage(session);
        when(sender.createMimeMessage()).thenReturn(realMessage);
        when(mailUtils.getUserName("uw01")).thenReturn("Test User");

        // Stub the directory lookup used by fetchVUserById inside buildMessage
        UserDto replyUser = new UserDto("alice@swissre.com", "Alice", "Smith");
        when(userDirectoryService.lookupByUserId("alice@swissre.com")).thenReturn(replyUser);

        Map<String, Object> data = new HashMap<>();
        data.put("UW_NAME", "Test User");
        data.put(MailConstants.DEAL_URL, "http://deal.local/D-001");
        data.put(MailConstants.DEAL_NUM, "D-001");
        data.put(MailConstants.RECEIPENTS, List.of("alice@swissre.com"));
        data.put(MailConstants.CC_RECEIPENTS, Collections.emptyList());
        data.put(MailConstants.RECEIVER_GROUP_MAIL, null);
        data.put(MailConstants.CC_RECEPIENT_GROUP, null);

        mailBO.sendMail(template, data, "uw01", "alice@swissre.com", null);

        // The HTML content is set directly on the message (no MimeMultipart wrapper).
        // Verify it can be serialized to bytes — this is the exact operation that
        // previously threw "no object DCH for MIME type multipart/mixed".
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertThatCode(() -> realMessage.writeTo(out))
                .as("MimeMessage.writeTo() must not throw UnsupportedDataTypeException")
                .doesNotThrowAnyException();

        String raw = out.toString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(raw).contains("<p>Hello Test User</p>");
        // Verify content type is text/html, NOT multipart/mixed
        assertThat(realMessage.getContentType()).startsWith("text/html");
    }

    // ── sendMailNotification ──────────────────────────────────────────────────

    @Test
    @DisplayName("sendMailNotification wraps unexpected exceptions as MailException")
    void sendMailNotification_repositoryThrows_wrapsAsMailException() {
        Map<String, Object> obj = buildMailCreationObject();
        when(templateRepository.getMailTemplate("POLICY_CREATION"))
                .thenThrow(new RuntimeException("DB down"));

        assertThatThrownBy(() -> mailBO.sendMailNotification(obj))
                .isInstanceOf(MailException.class)
                .hasMessageContaining("Error occurred while sending notification mail");
    }

    @Test
    @DisplayName("sendMailNotification re-throws MailException from sendMail without wrapping it")
    void sendMailNotification_mailExceptionBubbles_unchanged() throws Exception {
        TTemplate template = new TTemplate();
        template.setTemplateBody("Body");
        template.setTemplateSubject("Subject");

        when(templateRepository.getMailTemplate("POLICY_CREATION")).thenReturn(template);
        when(notificationPreferencesRepository.fetchUserToSendMails(any(), any())).thenReturn(null);
        when(mailUtils.getUserName(anyString())).thenReturn("Test User");

        MimeMessage mockMsg = mock(MimeMessage.class);
        when(sender.createMimeMessage()).thenReturn(mockMsg);
        doThrow(new MessagingException("session not configured")).when(mockMsg).setSubject(any());

        assertThatThrownBy(() -> mailBO.sendMailNotification(buildMailCreationObject()))
                .isInstanceOf(MailException.class)
                .hasMessageContaining(
                        "sending the mail") // S125: removed code-fragment inline comments
                .hasMessageNotContaining("Error occurred while sending notification mail");
    }

    @Test
    @DisplayName("sendMailNotification completes successfully with mocked mail infrastructure")
    void sendMailNotification_happyPath_completes() throws Exception {
        TTemplate template = new TTemplate();
        template.setTemplateBody("Dear ${UW_NAME}");
        template.setTemplateSubject("Deal ${DEAl_NUMBER}");

        when(templateRepository.getMailTemplate("POLICY_CREATION")).thenReturn(template);
        when(notificationPreferencesRepository.fetchUserToSendMails(any(), any())).thenReturn(null);
        // getUserName must not return null — "From :".concat(null) throws NPE
        when(mailUtils.getUserName(anyString())).thenReturn("Test User");

        MimeMessage mockMsg = mock(MimeMessage.class);
        when(sender.createMimeMessage()).thenReturn(mockMsg);
        when(mockMsg.getRecipients(Message.RecipientType.TO)).thenReturn(null);
        when(mockMsg.getRecipients(Message.RecipientType.CC)).thenReturn(null);

        mailBO.sendMailNotification(buildMailCreationObject());

        // No exception thrown and sender.send was NOT called (no recipients configured)
        verify(sender, never()).send(any(MimeMessage.class));
    }

    // ── sendTemplateMail ──────────────────────────────────────────────────────

    @Test
    @DisplayName("sendTemplateMail throws when the template does not exist")
    void sendTemplateMail_templateMissing_throws() {
        when(templateRepository.getMailTemplate("NO_SUCH_TEMPLATE")).thenReturn(null);

        assertThatThrownBy(
                        () ->
                                mailBO.sendTemplateMail(
                                        "NO_SUCH_TEMPLATE",
                                        new HashMap<>(),
                                        List.of("uw01"),
                                        Collections.emptyList()))
                .isInstanceOf(MailException.class)
                .hasMessageContaining("Mail template not found: NO_SUCH_TEMPLATE");

        verifyNoInteractions(sender);
    }

    @Test
    @DisplayName("sendTemplateMail puts the TO and CC lists into the placeholder map")
    void sendTemplateMail_populatesRecipientPlaceholders() throws Exception {
        stubTemplateMailInfrastructure();

        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("UW_NAME", "Test User");

        mailBO.sendTemplateMail(
                "GENERIC", placeholders, List.of("alice@swissre.com"), List.of("bob@swissre.com"));

        assertThat(placeholders)
                .containsEntry(MailConstants.RECEIPENTS, List.of("alice@swissre.com"))
                .containsEntry(MailConstants.CC_RECEIPENTS, List.of("bob@swissre.com"));
    }

    @Test
    @DisplayName("sendTemplateMail tolerates a null placeholder map")
    void sendTemplateMail_nullPlaceholders_doesNotThrow() throws Exception {
        stubTemplateMailInfrastructure();

        assertThatCode(
                        () ->
                                mailBO.sendTemplateMail(
                                        "GENERIC", null, List.of("alice@swissre.com"), null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("sendTemplateMail treats null recipient lists as empty")
    void sendTemplateMail_nullRecipientLists_treatedAsEmpty() throws Exception {
        stubTemplateMailInfrastructure();

        Map<String, Object> placeholders = new HashMap<>();

        mailBO.sendTemplateMail("GENERIC", placeholders, null, null);

        assertThat(placeholders)
                .containsEntry(MailConstants.RECEIPENTS, Collections.emptyList())
                .containsEntry(MailConstants.CC_RECEIPENTS, Collections.emptyList());
        verify(sender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendTemplateMail wraps unexpected failures as MailException")
    void sendTemplateMail_unexpectedFailure_wrapped() {
        when(templateRepository.getMailTemplate("GENERIC"))
                .thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(
                        () ->
                                mailBO.sendTemplateMail(
                                        "GENERIC", new HashMap<>(), List.of("uw01"), null))
                .isInstanceOf(MailException.class)
                .hasMessageContaining("Error sending templated mail 'GENERIC'")
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    /** Stubs the template lookup and mail plumbing shared by the sendTemplateMail happy paths. */
    private void stubTemplateMailInfrastructure() throws Exception {
        TTemplate template = new TTemplate();
        template.setTemplateBody("Dear ${UW_NAME}");
        template.setTemplateSubject("Generic subject");
        when(templateRepository.getMailTemplate("GENERIC")).thenReturn(template);

        MimeMessage mockMsg = mock(MimeMessage.class);
        when(sender.createMimeMessage()).thenReturn(mockMsg);
        when(mockMsg.getRecipients(Message.RecipientType.TO)).thenReturn(null);
        when(mockMsg.getRecipients(Message.RecipientType.CC)).thenReturn(null);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Map<String, Object> buildMailCreationObject() {
        Map<String, Object> obj = new HashMap<>();
        obj.put("templateName", "POLICY_CREATION");
        obj.put("underwriterId", "uw01");
        obj.put("uwAssistantId", "ua01");
        obj.put("attributeId", "10580");
        obj.put("underwriterName", "John Doe");
        obj.put("dealNumber", "D-001");
        obj.put("insuredName", "Acme Corp");
        obj.put("brokerName", null);
        return obj;
    }
}
