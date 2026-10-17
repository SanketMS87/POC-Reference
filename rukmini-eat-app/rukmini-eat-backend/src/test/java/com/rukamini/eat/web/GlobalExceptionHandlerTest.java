package com.rukamini.eat.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.rukamini.eat.service.ApiException;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void apiExceptionsKeepTheirStatusAndMessage() {
        for (HttpStatus status : new HttpStatus[] {HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED,
                HttpStatus.NOT_FOUND, HttpStatus.CONFLICT}) {
            var response = handler.handleApi(new ApiException(status, "boom " + status.value()));
            var body = Objects.requireNonNull(response.getBody());
            assertEquals(status, response.getStatusCode());
            assertEquals("boom " + status.value(), body.get("message"));
            assertEquals(1, body.size());
        }
    }
}
