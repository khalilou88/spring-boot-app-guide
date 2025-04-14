package com.example.api.exception;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
public class ValidationErrorDetails {
    private LocalDateTime timestamp;
    private String message;
    private Map<String, String> errors;

    public ValidationErrorDetails(LocalDateTime timestamp, String message, Map<String, String> errors) {
        this.timestamp = timestamp;
        this.message = message;
        this.errors = errors;
    }
}