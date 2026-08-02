package com.project.agent.adapter.in.rest.dto;

import java.time.Instant;

/** Uniform error body returned by {@code GlobalExceptionHandler}. */
public record ErrorResponse(
        String code,
        String message,
        Instant timestamp
) {
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, Instant.now());
    }
}
