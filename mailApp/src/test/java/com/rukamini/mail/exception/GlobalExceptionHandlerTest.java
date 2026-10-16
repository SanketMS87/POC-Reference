package com.rukamini.mail.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 *
 * Each exception-handler method is exercised directly — no Spring context needed.
 * The two conditions in {@code handleMailException} (httpStatus set / null) and
 * {@code handleGeneric} (message present / null) are both covered.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Mock private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    // ── handleMailException ───────────────────────────────────────────────────

    @Test
    @DisplayName("handleMailException with httpStatus set uses that status code")
    void handleMailException_withHttpStatus_usesIt() {
        MailException ex = new MailException("not found", HttpStatus.NOT_FOUND);

        ResponseEntity<ErrorResponse> resp = handler.handleMailException(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getMessage()).isEqualTo("not found");
        assertThat(resp.getBody().getPath()).isEqualTo("/api/test");
    }

    @Test
    @DisplayName("handleMailException with null httpStatus falls back to 500")
    void handleMailException_nullHttpStatus_returns500() {
        MailException ex = new MailException("generic error");

        ResponseEntity<ErrorResponse> resp = handler.handleMailException(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resp.getBody().getMessage()).isEqualTo("generic error");
    }

    // ── handleMethodArgumentNotValid ──────────────────────────────────────────

    @Test
    @DisplayName("handleMethodArgumentNotValid returns 400 with field-level error details")
    void handleMethodArgumentNotValid_returns400WithFieldErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult br = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(br);
        when(br.getFieldErrors())
                .thenReturn(List.of(new FieldError("obj", "fieldA", "must not be blank")));

        ResponseEntity<ErrorResponse> resp = handler.handleMethodArgumentNotValid(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getDetails()).containsExactly("fieldA: must not be blank");
    }

    // ── handleConstraintViolation ─────────────────────────────────────────────

    @Test
    @DisplayName("handleConstraintViolation returns 400 with constraint violation details")
    void handleConstraintViolation_returns400WithDetails() {
        ConstraintViolation<?> cv = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("initiate.dealNum");
        when(cv.getPropertyPath()).thenReturn(path);
        when(cv.getMessage()).thenReturn("must not be blank");
        ConstraintViolationException ex = new ConstraintViolationException(Set.of(cv));

        ResponseEntity<ErrorResponse> resp = handler.handleConstraintViolation(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getDetails()).hasSize(1);
        assertThat(resp.getBody().getDetails().get(0)).contains("must not be blank");
    }

    // ── handleNotReadable ─────────────────────────────────────────────────────

    @Test
    @DisplayName("handleNotReadable returns 400 with fixed 'Malformed JSON' message")
    void handleNotReadable_returns400WithFixedMessage() {
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);

        ResponseEntity<ErrorResponse> resp = handler.handleNotReadable(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getMessage()).isEqualTo("Malformed JSON request body");
    }

    // ── handleMissingParam ────────────────────────────────────────────────────

    @Test
    @DisplayName("handleMissingParam returns 400 with the missing parameter name in body")
    void handleMissingParam_returns400WithParamName() {
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("dealNum", "String");

        ResponseEntity<ErrorResponse> resp = handler.handleMissingParam(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getMessage()).contains("dealNum");
    }

    // ── handleTypeMismatch ────────────────────────────────────────────────────

    @Test
    @DisplayName("handleTypeMismatch returns 400 with parameter name in message")
    void handleTypeMismatch_returns400WithParamName() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("policyNum");

        ResponseEntity<ErrorResponse> resp = handler.handleTypeMismatch(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getMessage()).contains("policyNum");
    }

    // ── handleMethodNotSupported ──────────────────────────────────────────────

    @Test
    @DisplayName("handleMethodNotSupported returns 405")
    void handleMethodNotSupported_returns405() {
        HttpRequestMethodNotSupportedException ex =
                new HttpRequestMethodNotSupportedException("DELETE");

        ResponseEntity<ErrorResponse> resp = handler.handleMethodNotSupported(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(resp.getBody().getStatus()).isEqualTo(405);
    }

    // ── handleAccessDenied ────────────────────────────────────────────────────

    @Test
    @DisplayName("handleAccessDenied returns 403")
    void handleAccessDenied_returns403() {
        AccessDeniedException ex = new AccessDeniedException("Forbidden");

        ResponseEntity<ErrorResponse> resp = handler.handleAccessDenied(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody().getMessage()).isEqualTo("Forbidden");
    }

    // ── handleNoResourceFound ─────────────────────────────────────────────────

    @Test
    @DisplayName("handleNoResourceFound returns 404 with the request URI in body")
    void handleNoResourceFound_returns404WithPath() {
        NoResourceFoundException ex = mock(NoResourceFoundException.class);
        when(ex.getMessage()).thenReturn("No static resource api/test");

        ResponseEntity<ErrorResponse> resp = handler.handleNoResourceFound(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody().getMessage()).contains("/api/test");
        assertThat(resp.getBody().getStatus()).isEqualTo(404);
    }

    // ── handleGeneric ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("handleGeneric with a non-null message returns 500 with that message")
    void handleGeneric_withMessage_returns500() {
        Exception ex = new RuntimeException("something broke");

        ResponseEntity<ErrorResponse> resp = handler.handleGeneric(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resp.getBody().getMessage()).isEqualTo("something broke");
    }

    @Test
    @DisplayName(
            "handleGeneric with null exception message returns 500 with 'Unexpected error' fallback")
    void handleGeneric_nullMessage_returns500WithFallback() {
        Exception ex = new RuntimeException((String) null);

        ResponseEntity<ErrorResponse> resp = handler.handleGeneric(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resp.getBody().getMessage()).isEqualTo("Unexpected error");
    }
}
