package com.example.floodgate.exception;

import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.Map;

public record ErrorResponse(
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors,
        OffsetDateTime timestamp
) {
    public static ErrorResponse of(HttpStatus status, String message, Map<String, String> fieldErrors) {
        return new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                fieldErrors,
                OffsetDateTime.now()
        );
    }
}
