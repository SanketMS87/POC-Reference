package com.rukamini.mail.exception;

import org.springframework.http.HttpStatus;

public class MailException extends Exception {

    public MailException(String msg) {
        super(msg);
    }

    public MailException(String msg, HttpStatus httpStatus) {
        super(msg);
        setHttpStatus(httpStatus);
    }

    private static final long serialVersionUID = 1L;

    private String errorCode;
    private HttpStatus httpStatus;

    /**
     * @param detail
     *            The detail message.
     */
    public MailException(String detail, int httpResponseStatus) {
        this(detail, null, httpResponseStatus);
    }

    /**
     * @param detail
     *            The detail message.
     * @param rootCause
     *            The Throwable that caused this exception to be thrown.
     */
    public MailException(String detail, Throwable rootCause) {
        this(detail, rootCause, null);
    }

    /**
     * @param detail
     *            The detail message.
     * @param rootCause
     *            The Throwable that caused this exception to be thrown.
     */
    public MailException(String detail, Throwable rootCause, int httpResponseStatus) {
        this(detail, rootCause, null, httpResponseStatus);
    }

    /**
     * @param detail
     *            The detail message.
     * @param rootCause
     *            The Throwable that caused this exception to be thrown.
     * @param operationId
     *            The operation's id.
     */
    public MailException(String detail, Throwable rootCause, String errorCode) {
        super(detail, rootCause);
        setErrorCode(errorCode);
    }

    /**
     * @param detail
     *            The detail message.
     * @param rootCause
     *            The Throwable that caused this exception to be thrown.
     * @param operationId
     *            The operation's id.
     */
    public MailException(
            String detail, Throwable rootCause, String errorCode, int httpResponseStatus) {
        super(detail, rootCause);
        setErrorCode(errorCode);
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }
}
