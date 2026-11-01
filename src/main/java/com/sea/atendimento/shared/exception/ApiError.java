package com.sea.atendimento.shared.exception;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldErrorDetail> fieldErrors
) {
    public static ApiError of(HttpStatus status, String message, String path,
                              List<FieldErrorDetail> fieldErrors) {
        return new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                List.copyOf(fieldErrors)
        );
    }

    public static ApiError of(HttpStatus status, String message, String path) {
        return of(status, message, path, List.of());
    }
}
