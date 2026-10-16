package com.rukamini.mail.controller.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for the {@link SendEmailRequest} payload. */
@DisplayName("SendEmailRequest DTO")
class SendEmailRequestTest {

    private SendEmailRequest request;

    @BeforeEach
    void setUp() {
        request = new SendEmailRequest();
    }

    @Test
    @DisplayName("all fields are null on a freshly constructed instance")
    void testDefaultsAreNull() {
        assertThat(request.getDealNumber()).isNull();
        assertThat(request.getTemplateName()).isNull();
        assertThat(request.getAssigneeUserId()).isNull();
        assertThat(request.getDocumentName()).isNull();
    }

    @Test
    @DisplayName("dealNumber round-trips through its accessor")
    void testDealNumberAccessor() {
        request.setDealNumber("D12345");
        assertThat(request.getDealNumber()).isEqualTo("D12345");
    }

    @Test
    @DisplayName("templateName round-trips through its accessor")
    void testTemplateNameAccessor() {
        request.setTemplateName("UW_AUTHORITY_SELF_DECLARATION");
        assertThat(request.getTemplateName()).isEqualTo("UW_AUTHORITY_SELF_DECLARATION");
    }

    @Test
    @DisplayName("assigneeUserId round-trips through its accessor")
    void testAssigneeUserIdAccessor() {
        request.setAssigneeUserId("S-USER123");
        assertThat(request.getAssigneeUserId()).isEqualTo("S-USER123");
    }

    @Test
    @DisplayName("documentName round-trips through its accessor")
    void testDocumentNameAccessor() {
        request.setDocumentName("SelfDeclaration.pdf");
        assertThat(request.getDocumentName()).isEqualTo("SelfDeclaration.pdf");
    }

    @Test
    @DisplayName("documentName is optional and accepts null")
    void testDocumentNameIsOptional() {
        request.setDocumentName("SelfDeclaration.pdf");
        request.setDocumentName(null);
        assertThat(request.getDocumentName()).isNull();
    }

    @Test
    @DisplayName("a fully populated request exposes every value independently")
    void testFullyPopulatedRequest() {
        request.setDealNumber("D999");
        request.setTemplateName("UW_AUTHORITY_SELF_DECLARATION");
        request.setAssigneeUserId("S-UW001");
        request.setDocumentName("Declaration.pdf");

        assertThat(request.getDealNumber()).isEqualTo("D999");
        assertThat(request.getTemplateName()).isEqualTo("UW_AUTHORITY_SELF_DECLARATION");
        assertThat(request.getAssigneeUserId()).isEqualTo("S-UW001");
        assertThat(request.getDocumentName()).isEqualTo("Declaration.pdf");
    }
}
