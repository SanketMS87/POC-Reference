package com.rukamini.mail.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.rukamini.mail.controller.dto.SendEmailRequest;
import com.rukamini.mail.dao.NotificationBO;
import com.rukamini.mail.exception.MailException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link NotificationController}. */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationController")
class NotificationControllerTest {

    @Mock private NotificationBO notificationBO;

    @InjectMocks private NotificationController controller;

    private SendEmailRequest request;

    @BeforeEach
    void setUp() {
        request = new SendEmailRequest();
        request.setTemplateName("UW_AUTHORITY_SELF_DECLARATION");
        request.setDealNumber("D12345");
        request.setAssigneeUserId("S-USER123");
        request.setDocumentName("SelfDeclaration.pdf");
    }

    @Test
    @DisplayName("sendEmail delegates the request to NotificationBO")
    void testSendEmailDelegates() throws MailException {
        controller.sendEmail(request);

        verify(notificationBO).sendEmail(request);
        verifyNoMoreInteractions(notificationBO);
    }

    @Test
    @DisplayName("sendEmail propagates MailException raised by NotificationBO")
    void testSendEmailPropagatesMailException() throws MailException {
        MailException expected = new MailException("Failed to send email");
        doThrow(expected).when(notificationBO).sendEmail(any());

        assertThatThrownBy(() -> controller.sendEmail(request)).isSameAs(expected);
    }

    @Test
    @DisplayName("sendEmail delegates even when documentName is absent")
    void testSendEmailWithoutDocumentName() throws MailException {
        request.setDocumentName(null);

        controller.sendEmail(request);

        verify(notificationBO).sendEmail(request);
    }
}
