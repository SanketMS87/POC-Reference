package com.rukamini.mail.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.rukamini.mail.user.UserDirectoryService;
import com.rukamini.mail.user.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("MailUtils")
class MailUtilsTest {

    @Mock private UserDirectoryService userDirectoryService;

    private MailUtils mailUtils;

    @BeforeEach
    void setUp() {
        mailUtils = new MailUtils(userDirectoryService);
    }

    // ── getUserName ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getUserName returns 'LastName,FirstName' for a found user")
    void getUserName_userFound_returnsFormattedName() {
        when(userDirectoryService.lookupByUserId("jsmith"))
                .thenReturn(new UserDto("jsmith@swissre.com", "John", "Smith"));

        String result = mailUtils.getUserName("jsmith");

        assertThat(result).isEqualTo("Smith,John");
    }

    @Test
    @DisplayName("getUserName returns empty string when user not found")
    void getUserName_userNotFound_returnsEmpty() {
        when(userDirectoryService.lookupByUserId("nobody")).thenReturn(null);

        String result = mailUtils.getUserName("nobody");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getUserName handles id with '#~#' separator")
    void getUserName_idWithSeparator_passesIdToFetch() {
        when(userDirectoryService.lookupByUserId(any()))
                .thenReturn(new UserDto("jdoe@swissre.com", "Jane", "Doe"));

        String result = mailUtils.getUserName("jdoe#~#someExtra");

        assertThat(result).isEqualTo("Doe,Jane");
    }
}
