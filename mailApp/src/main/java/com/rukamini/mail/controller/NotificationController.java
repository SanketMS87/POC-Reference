package com.rukamini.mail.controller;

import com.rukamini.mail.controller.dto.SendEmailRequest;
import com.rukamini.mail.dao.NotificationBO;
import com.rukamini.mail.exception.MailException;
import com.rukamini.mail.filter.ResponseCorsFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Generic mail-triggering endpoint. Dispatch is driven by
 * {@link SendEmailRequest#getTemplateName()} ΓÇö see {@link NotificationBO}.
 *
 * <p>CORS is configured globally via {@link ResponseCorsFilter} to ensure a safe,
 * restricted origin policy — only requests from *.swissre.com, *.swissreapps-np.com, or
 * localhost are allowed.
 */
@RestController
@RequestMapping("/api")
@Validated
public class NotificationController {

    @Autowired NotificationBO notificationBO;

    @ResponseStatus(HttpStatus.OK)
    @PostMapping(path = "/sendEmail", consumes = "application/json", produces = "application/json")
    @ResponseBody
    @Operation(summary = "Trigger an email based on templateName (generic dispatch)")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "200", description = "Mail sent successfully"),
                @ApiResponse(responseCode = "400", description = "Bad Request"),
                @ApiResponse(responseCode = "403", description = "Access Forbidden")
            })
    @PreAuthorize("hasAnyRole(@allUserRolesWithReadWrite)")
    public void sendEmail(@RequestBody @Valid SendEmailRequest request) throws MailException {
        notificationBO.sendEmail(request);
    }
}
