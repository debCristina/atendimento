package com.sea.atendimento.shared.exception;


import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FieldErrorDetail(Integer step, String field, String message) {

    public static FieldErrorDetail of(String field, String message) {
        return new FieldErrorDetail(null, field, message);
    }

    public static FieldErrorDetail ofStep(int step, String field, String message) {
        return new FieldErrorDetail(step, field, message);
    }
}
