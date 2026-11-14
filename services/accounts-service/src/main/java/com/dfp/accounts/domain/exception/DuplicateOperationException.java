package com.dfp.accounts.domain.exception;

public class DuplicateOperationException extends RuntimeException {
    public DuplicateOperationException(String message) {
        super(message);
    }
}
