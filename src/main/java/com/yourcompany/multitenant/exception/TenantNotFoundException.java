// TenantNotFoundException.java
package com.yourcompany.multitenant.exception;

public class TenantNotFoundException extends RuntimeException {
    public TenantNotFoundException(String message) {
        super(message);
    }
}