package com.sea.atendimento.shared.exception;

import java.util.List;

public class BusinessRuleException extends RuntimeException {

    private final List<FieldErrorDetail> fieldErrors;

    public BusinessRuleException(String message) {
        this(message, List.of());
    }

    public BusinessRuleException(String message, List<FieldErrorDetail> fieldErrors) {
        super(message);
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public List<FieldErrorDetail> getFieldErrors() {
        return fieldErrors;
    }
}