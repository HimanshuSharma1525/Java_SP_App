// AuthService.java
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

    @Transactional
    public LoginResponse login(LoginRequest request) {
        // Authenticate
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // Get user
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

        // Determine redirect URL based on role
        String redirectUrl = getRedirectUrl(user.getRole());

        return LoginResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole().name())
                .redirectUrl(redirectUrl)
                .userId(user.getId())
                .tenantId(user.getTenant().getId())
                .build();
    }

    private void validateTenantAccess(User user) {
        String currentSubdomain = TenantContext.getTenantId();
        String userSubdomain = user.getTenant().getSubdomain();

        // SUPER_ADMIN can ONLY login via superadmin subdomain
        if (user.getRole() == Role.SUPER_ADMIN) {
            if (!"superadmin".equals(currentSubdomain)) {
                throw new UnauthorizedAccessException(
                        "Super admin can only login via superadmin subdomain"
                );
            }
        }
        // CUSTOMER_ADMIN can loginn via superadmin OR their own subdomain
        else if (user.getRole() == Role.CUSTOMER_ADMIN) {
            if (!"superadmin".equals(currentSubdomain) && !userSubdomain.equals(currentSubdomain)) {
                throw new UnauthorizedAccessException(
                        "Customer admin can only login via superadmin or their subdomain"
                );
            }
        }
        // END_USER can ONLY loginn via their tenant's subdomain
        else if (user.getRole() == Role.END_USER) {
            if (!userSubdomain.equals(currentSubdomain)) {
                throw new UnauthorizedAccessException(
                        "End user can only login via their tenant subdomain: " + userSubdomain
                );
            }
        }
    }

    private String getRedirectUrl(Role role) {
        return switch (role) {
            case SUPER_ADMIN -> "/super-admin/dashboard";
            case CUSTOMER_ADMIN -> "/customer-admin/dashboard";
            case END_USER -> "/end-user/dashboard";
        };
    }

    @Transactional
    public User register(RegisterRequest request) {
        // Registration is only allowed for END_USER from customer admin subdomains
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