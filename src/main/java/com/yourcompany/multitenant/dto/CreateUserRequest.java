// CreateUserRequest.java
package com.yourcompany.multitenant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateUserRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank
    private String role; // CUSTOMER_ADMIN or END_USER

    private String tenantSubdomain; // Required for CUSTOMER_ADMIN creation by SUPER_ADMIN
}