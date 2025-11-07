//// JwtSSOService.java
//package com.yourcompany.multitenant.service;
//
//import com.yourcompany.multitenant.dto.LoginResponse;
//import com.yourcompany.multitenant.exception.SSOAuthenticationException;
//import com.yourcompany.multitenant.model.*;
//import com.yourcompany.multitenant.repository.SSOConfigRepository;
//import com.yourcompany.multitenant.repository.UserRepository;
//import com.yourcompany.multitenant.security.JwtTokenProvider;
//import io.jsonwebtoken.Claims;
//import io.jsonwebtoken.Jwts;
//import io.jsonwebtoken.security.Keys;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.web.reactive.function.client.WebClient;
//import javax.crypto.SecretKey;
//import java.nio.charset.StandardCharsets;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class JwtSSOService {
//
//    private final SSOConfigRepository ssoConfigRepository;
//    private final UserRepository userRepository;
//    private final TenantService tenantService;
//    private final JwtTokenProvider jwtTokenProvider;
//    private final WebClient.Builder webClientBuilder;
//
//    public String getAuthorizationUrl() {
//        Tenant tenant = tenantService.getCurrentTenant();
//        SSOConfig config = ssoConfigRepository.findByTenantAndProvider(tenant, SSOProvider.JWT)
//                .orElseThrow(() -> new SSOAuthenticationException("JWT SSO not configured"));
//
//        if (!config.getEnabled()) {
//            throw new SSOAuthenticationException("JWT SSO is not enabled");
//        }
//
//        return config.getJwtTokenEndpoint();
//    }
//
//    public LoginResponse authenticate(String idpToken) {
//        Tenant tenant = tenantService.getCurrentTenant();
//        SSOConfig config = ssoConfigRepository.findByTenantAndProvider(tenant, SSOProvider.JWT)
//                .orElseThrow(() -> new SSOAuthenticationException("JWT SSO not configured"));
//
//        if (!config.getEnabled()) {
//            throw new SSOAuthenticationException("JWT SSO is not enabled");
//        }
//
//        try {
//            // Validate JWT token from MinioRange using HS256
//            SecretKey key = Keys.hmacShaKeyFor(config.getJwtSecret().getBytes(StandardCharsets.UTF_8));
//
//            Claims claims = Jwts.parserBuilder()
//                    .setSigningKey(key)
//                    .requireIssuer(config.getJwtIssuer())
//                    .build()
//                    .parseClaimsJws(idpToken)
//                    .getBody();
//
//            String email = claims.get("email", String.class);
//
//            // Get or create user
//            User user = userRepository.findByEmail(email)
//                    .orElseThrow(() -> new SSOAuthenticationException("User not found: " + email));
//
//            // Validate tenant
//            if (!user.getTenant().getId().equals(tenant.getId())) {
//                throw new SSOAuthenticationException("User does not belong to this tenant");
//            }
//
//            // Generate application JWT
//            String appToken = jwtTokenProvider.generateToken(
//                    user.getId(),
//                    user.getEmail(),
//                    user.getRole(),
//                    user.getTenant().getId()
//            );
//
//            String redirectUrl = getRedirectUrl(user.getRole());
//
//            return LoginResponse.builder()
//                    .token(appToken)
//                    .email(user.getEmail())
//                    .role(user.getRole().name())
//                    .redirectUrl(redirectUrl)
//                    .userId(user.getId())
//                    .tenantId(user.getTenant().getId())
//                    .build();
//
//        } catch (Exception e) {
//            log.error("JWT SSO authentication failed", e);
//            throw new SSOAuthenticationException("JWT SSO authentication failed: " + e.getMessage());
//        }
//    }
//
//    private String getRedirectUrl(Role role) {
//        return switch (role) {
//            case SUPER_ADMIN -> "/super-admin/dashboard";
//            case CUSTOMER_ADMIN -> "/customer-admin/dashboard";
//            case END_USER -> "/end-user/dashboard";
//        };
//    }
//}