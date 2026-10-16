package com.rukamini.mail.util;

import com.rukamini.mail.user.UserDirectoryService;
import com.rukamini.mail.user.UserDto;
import org.springframework.stereotype.Component;

@Component
public class MailUtils {

    final UserDirectoryService userDirectoryService;

    public MailUtils(UserDirectoryService userDirectoryService) {
        this.userDirectoryService = userDirectoryService;
    }

    public String getUserName(String id) {
        UserDto user = userDirectoryService.lookupByUserId(id);
        return null != user ? user.lastName().concat(",").concat(user.firstName()) : "";
    }
}
