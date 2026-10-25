package com.dfp.accounts.domain.exception;

public class InvalidAccountStateException extends RuntimeException {
    public InvalidAccountStateException(String message) {
        super(message);
    }
}
