package com.yourcompany.multitenant.controller;

import com.yourcompany.multitenant.dto.LoginRequest;
import com.yourcompany.multitenant.dto.LoginResponse;
import com.yourcompany.multitenant.dto.CreateUserRequest; // 🌟 Using CreateUserRequest for flexibility
import com.yourcompany.multitenant.model.User;
import com.yourcompany.multitenant.service.AuthService; // NOTE: We'll route through AuthService
import com.yourcompany.multitenant.service.UserService; // 🌟 Injecting UserService directly for clearer routing
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus; // Import HttpStatus
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    // You must change the field to access the necessary registration methods
    private final UserService userService; // 🌟 Use UserService for registration
    private final AuthService authService; // Keep AuthService for login

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/login-cookie")
    public ResponseEntity<LoginResponse> loginWithCookie(@Valid @RequestBody LoginRequest request,
                                                         HttpServletResponse response) {
        return ResponseEntity.ok(authService.loginWithCookie(request, response));
    }

    /**
     * 🟢 Dual-Purpose Registration Endpoint:
     * - If tenantSubdomain is present, registers a Customer Admin (new organization).
     * - If tenantSubdomain is absent, registers an End User (current tenant).
     */
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody CreateUserRequest request) { // 🌟 Use CreateUserRequest
        User createdUser;

        if (request.getTenantSubdomain() != null && !request.getTenantSubdomain().isEmpty()) {
            // 1. Customer Admin Registration (from Super Admin domain)
            // This method handles tenant creation/lookup and sets role to CUSTOMER_ADMIN
            userService.createCustomerAdmin(request);
            return ResponseEntity.status(HttpStatus.CREATED).body("Customer Admin and Tenant registered successfully.");
        } else {
            // 2. End User Registration (from a specific tenant domain)
            // This method handles setting the role to END_USER and using the current tenant context.
            userService.createEndUser(request);
            return ResponseEntity.status(HttpStatus.CREATED).body("End User registered successfully.");
        }
    }
}