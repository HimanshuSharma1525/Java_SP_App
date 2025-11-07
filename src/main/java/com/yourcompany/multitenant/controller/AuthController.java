package com.yourcompany.multitenant.controller;

import com.yourcompany.multitenant.dto.LoginRequest;
import com.yourcompany.multitenant.dto.LoginResponse;
import com.yourcompany.multitenant.dto.RegisterRequest;
import com.yourcompany.multitenant.model.User;
import com.yourcompany.multitenant.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/login-cookie")
    public ResponseEntity<LoginResponse> loginWithCookie(@Valid @RequestBody LoginRequest request,
                                                         HttpServletResponse response) {
        return ResponseEntity.ok(authService.loginWithCookie(request, response));
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request);
        return ResponseEntity.ok("User registered successfully with ID: " + user.getId());
    }
}
