package com.rukamini.eat.web;

import io.swagger.v3.oas.annotations.media.Schema;

/** Documents the error body returned by {@link GlobalExceptionHandler}. */
@Schema(description = "Application validation or business error")
public record ApiError(@Schema(example = "Invalid email or password") String message) {
}
