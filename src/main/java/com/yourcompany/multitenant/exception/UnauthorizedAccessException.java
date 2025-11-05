// UnauthorizedAccessException.java
package com.yourcompany.multitenant.exception;

public class UnauthorizedAccessException extends RuntimeException {
    public UnauthorizedAccessException(String message) {
        super(message);
    }
}