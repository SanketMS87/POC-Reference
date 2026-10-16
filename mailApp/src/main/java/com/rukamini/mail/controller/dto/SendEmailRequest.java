package com.rukamini.mail.controller.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload for the generic {@code POST /api/sendEmail} endpoint.
 *
 * <p>{@code templateName} drives dispatch inside
 * {@link com.rukamini.mail.dao.NotificationBO} ΓÇö each supported template name
 * has a dedicated builder that knows which upstream data to fetch and which
 * placeholders to populate.
 */
public class SendEmailRequest {

    @NotBlank private String dealNumber;

    @NotBlank private String templateName;

    @NotBlank private String assigneeUserId;

    // Optional ΓÇö required by UW_AUTHORITY_SELF_DECLARATION; ignored by templates that don't
    // reference ${DocumentName}.
    private String documentName;

    public String getDealNumber() {
        return dealNumber;
    }

    public void setDealNumber(String dealNumber) {
        this.dealNumber = dealNumber;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getAssigneeUserId() {
        return assigneeUserId;
    }

    public void setAssigneeUserId(String assigneeUserId) {
        this.assigneeUserId = assigneeUserId;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }
}
