package com.rukamini.mail.controller;

import com.rukamini.mail.dao.MailHelperBO;
import com.rukamini.mail.exception.MailException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@Validated
public class MailHelperController {

    private final MailHelperBO mailHelperBO;

    public MailHelperController(MailHelperBO mailHelperBO) {
        this.mailHelperBO = mailHelperBO;
    }

    @ResponseStatus(HttpStatus.OK)
    @PostMapping(
            path = "/initiate/{dealNum}/{policyNum}/{templateName}",
            consumes = "application/json",
            produces = "application/json")
    @CrossOrigin(origins = "*")
    @Operation(summary = "Trigger mail based on policyNum, dealNum and template name")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "200", description = "Mail sent successfully"),
                @ApiResponse(responseCode = "400", description = "Bad Request"),
                @ApiResponse(responseCode = "403", description = "Access Forbidden")
            })
    @PreAuthorize("hasAnyRole(@allUserRoles)")
    public void sendMailNotification(
            @PathVariable @NotBlank String dealNum,
            @PathVariable @NotBlank String policyNum,
            @PathVariable @NotBlank String templateName)
            throws MailException {
        mailHelperBO.sendMailNotification(dealNum, policyNum, templateName);
    }

    @ResponseStatus(HttpStatus.OK)
    @PostMapping(
            path = "/initiate/{dealOrPolicyNum}/{templateName}",
            consumes = "application/json",
            produces = "application/json")
    @CrossOrigin(origins = "*")
    @Operation(summary = "Trigger mail based on dealNum and template name")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "200", description = "Mail sent successfully"),
                @ApiResponse(responseCode = "400", description = "Bad Request"),
                @ApiResponse(responseCode = "403", description = "Access Forbidden")
            })
    @PreAuthorize("hasAnyRole(@allUserRolesWithReadWrite)")
    public void sendDeclineDealMailNotification(
            @PathVariable @NotBlank String dealOrPolicyNum,
            @PathVariable @NotBlank String templateName)
            throws MailException {
        mailHelperBO.sendDeclineDealOrPolicyMailNotification(dealOrPolicyNum, templateName);
    }
}
