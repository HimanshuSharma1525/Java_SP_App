package com.yourcompany.multitenant.service;

import com.yourcompany.multitenant.config.TenantContext;
import com.yourcompany.multitenant.config.TenantFilter;
import com.yourcompany.multitenant.dto.LoginRequest;
import com.yourcompany.multitenant.dto.LoginResponse;
import com.yourcompany.multitenant.dto.RegisterRequest;
import com.yourcompany.multitenant.exception.DomainAccessException;
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
     * Generates JWT and performs tenant + role validation.
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

        // Validate tenant access
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
     * Helps align with SSO-based logins.
     */
    @Transactional
    public LoginResponse loginWithCookie(LoginRequest request, HttpServletResponse response) {
        LoginResponse loginResponse = login(request);

        Cookie jwtCookie = new Cookie("AUTH_TOKEN", loginResponse.getToken());
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(24 * 60 * 60); // 1 day
        response.addCookie(jwtCookie);

        log.info("JWT cookie issued for user {}", loginResponse.getEmail());
        return loginResponse;
    }

    /**
     * Validates that a user is logging in through the correct tenant or base domain.
     */
    private void validateTenantAccess(User user) {
        String currentTenant = TenantContext.getTenantId(); // e.g., SUPERADMIN or tenant1
        String userSubdomain = user.getTenant().getSubdomain(); // e.g., tenant1

        log.debug("Validating login for user: {}, role: {}, currentTenant: {}",
                user.getEmail(), user.getRole(), currentTenant);

        // 🟣 SUPER ADMIN — only via base domain
        if (user.getRole() == Role.SUPER_ADMIN) {
            if (!TenantFilter.SUPER_ADMIN_ID.equalsIgnoreCase(currentTenant)) {
                throw new DomainAccessException("Super admin can only login via base domain");
            }
        }

        // 🏢 CUSTOMER ADMIN — via base domain or their own subdomain
        else if (user.getRole() == Role.CUSTOMER_ADMIN) {
            if (!TenantFilter.SUPER_ADMIN_ID.equalsIgnoreCase(currentTenant)
                    && !userSubdomain.equalsIgnoreCase(currentTenant)) {
                throw new DomainAccessException(
                        "Customer admin can only login via base domain or their tenant domain"
                );
            }
        }

        // 👤 END USER — only via their tenant subdomain
        else if (user.getRole() == Role.END_USER) {
            if (!userSubdomain.equalsIgnoreCase(currentTenant)) {
                throw new DomainAccessException(
                        "End user can only login via their tenant domain: " + userSubdomain
                );
            }
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
        String currentTenant = TenantContext.getTenantId();

        // 🚫 Prevent registration from base domain
        if (TenantFilter.SUPER_ADMIN_ID.equalsIgnoreCase(currentTenant)) {
            throw new DomainAccessException("Cannot register users via base domain");
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
