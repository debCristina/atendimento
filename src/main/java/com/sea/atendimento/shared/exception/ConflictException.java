package com.sea.atendimento.shared.exception;


public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}