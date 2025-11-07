package com.yourcompany.multitenant.service;

import com.yourcompany.multitenant.config.TenantContext;
import com.yourcompany.multitenant.dto.LoginRequest;
import com.yourcompany.multitenant.dto.LoginResponse;
import com.yourcompany.multitenant.dto.RegisterRequest;
import com.yourcompany.multitenant.exception.UnauthorizedAccessException;
import com.yourcompany.multitenant.model.Role;
import com.yourcompany.multitenant.model.Tenant;
import com.yourcompany.multitenant.model.User;
import com.yourcompany.multitenant.repository.UserRepository;
import com.yourcompany.multitenant.security.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TenantService tenantService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    /**
     * Handles username/password login.
     * Generates JWT and performs tenant+role validation.
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        // Authenticate credentials
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // Fetch user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedAccessException("Invalid credentials"));

        // Validate tenant
        validateTenantAccess(user);

        // Generate token
        String token = tokenProvider.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getTenant().getId()
        );

        // Build response
        return LoginResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole().name())
                .redirectUrl(getRedirectUrl(user.getRole()))
                .userId(user.getId())
                .tenantId(user.getTenant().getId())
                .build();
    }

    /**
     * Version of login that also sets JWT as HttpOnly cookie.
     * This helps align behavior with SSO-based logins.
     */
    @Transactional
    public LoginResponse loginWithCookie(LoginRequest request, HttpServletResponse response) {
        LoginResponse loginResponse = login(request);

        Cookie jwtCookie = new Cookie("AUTH_TOKEN", loginResponse.getToken());
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(24 * 60 * 60);
        response.addCookie(jwtCookie);

        log.info("JWT cookie issued for user {}", loginResponse.getEmail());
        return loginResponse;
    }

    /**
     * Validates that a user is logging in through the correct tenant subdomain.
     */
    private void validateTenantAccess(User user) {
        String currentSubdomain = TenantContext.getTenantId();
        String userSubdomain = user.getTenant().getSubdomain();

        if (user.getRole() == Role.SUPER_ADMIN && !"superadmin".equals(currentSubdomain)) {
            throw new UnauthorizedAccessException("Super admin can only login via superadmin subdomain");
        }

        if (user.getRole() == Role.CUSTOMER_ADMIN) {
            if (!"superadmin".equals(currentSubdomain) && !userSubdomain.equals(currentSubdomain)) {
                throw new UnauthorizedAccessException("Customer admin can only login via superadmin or their subdomain");
            }
        }

        if (user.getRole() == Role.END_USER && !userSubdomain.equals(currentSubdomain)) {
            throw new UnauthorizedAccessException(
                    "End user can only login via their tenant subdomain: " + userSubdomain
            );
        }
    }

    /**
     * Maps roles to their respective dashboard URLs.
     */
    private String getRedirectUrl(Role role) {
        return switch (role) {
            case SUPER_ADMIN -> "/super-admin-dashboard.html";
            case CUSTOMER_ADMIN -> "/customer-admin-dashboard.html";
            case END_USER -> "/end-user-dashboard.html";
        };
    }

    /**
     * Registers a new user (END_USER only) for a given tenant.
     */
    @Transactional
    public User register(RegisterRequest request) {
        String subdomain = TenantContext.getTenantId();

        if ("superadmin".equals(subdomain)) {
            throw new UnauthorizedAccessException("Cannot register users via superadmin subdomain");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        Tenant tenant = tenantService.getCurrentTenant();

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(Role.END_USER)
                .tenant(tenant)
                .active(true)
                .build();

        return userRepository.save(user);
    }
}
