// SSOAuthenticationException.java
package com.yourcompany.multitenant.exception;

public class SSOAuthenticationException extends RuntimeException {
    public SSOAuthenticationException(String message) {
        super(message);
    }
}