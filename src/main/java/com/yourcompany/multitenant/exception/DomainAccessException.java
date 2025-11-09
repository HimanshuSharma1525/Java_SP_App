package com.yourcompany.multitenant.exception;

public class DomainAccessException extends RuntimeException {
    public DomainAccessException(String message) {
        super(message);
    }
}
