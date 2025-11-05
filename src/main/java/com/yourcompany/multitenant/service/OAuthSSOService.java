//// OAuthSSOService.java
//package com.yourcompany.multitenant.service;
//
//import com.yourcompany.multitenant.dto.LoginResponse;
//import com.yourcompany.multitenant.exception.SSOAuthenticationException;
//import com.yourcompany.multitenant.model.*;
//import com.yourcompany.multitenant.repository.SSOConfigRepository;
//import com.yourcompany.multitenant.repository.UserRepository;
//import com.yourcompany.multitenant.security.JwtTokenProvider;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.util.LinkedMultiValueMap;
//import org.springframework.util.MultiValueMap;
//import org.springframework.web.reactive.function.client.WebClient;
//import java.util.Map;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class OAuthSSOService {
//
//    private final SSOConfigRepository ssoConfigRepository;
//    private final UserRepository userRepository;
//    private final TenantService tenantService;
//    private final JwtTokenProvider jwtTokenProvider;
//    private final WebClient.Builder webClientBuilder;
//
//    public String getAuthorizationUrl() {
//        Tenant tenant = tenantService.getCurrentTenant();
//        SSOConfig config = ssoConfigRepository.findByTenantAndProvider(tenant, SSOProvider.OAUTH)
//                .orElseThrow(() -> new SSOAuthenticationException("OAuth SSO not configured"));
//
//        if (!config.getEnabled()) {
//            throw new SSOAuthenticationException("OAuth SSO is not enabled");
//        }
//
//        // Build OAuth authorization URL
//        return String.format("%s?client_id=%s&redirect_uri=%s&response_type=code&scope=%s",
//                config.getOauthAuthorizationUrl(),
//                config.getOauthClientId(),
//                config.getOauthRedirectUri(),
//                config.getOauthScopes()
//        );
//    }
//
//    public LoginResponse authenticate(String code) {
//        Tenant tenant = tenantService.getCurrentTenant();
//        SSOConfig config = ssoConfigRepository.findByTenantAndProvider(tenant, SSOProvider.OAUTH)
//                .orElseThrow(() -> new SSOAuthenticationException("OAuth SSO not configured"));
//
//        if (!config.getEnabled()) {
//            throw new SSOAuthenticationException("OAuth SSO is not enabled");
//        }
//
//        try {
//            // Exchange code for access token
//            WebClient webClient = webClientBuilder.build();
//
//            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
//            formData.add("grant_type", "authorization_code");
//            formData.add("code", code);
//            formData.add("redirect_uri", config.getOauthRedirectUri());
//            formData.add("client_id", config.getOauthClientId());
//            formData.add("client_secret", config.getOauthClientSecret());
//
//            Map<String, Object> tokenResponse = webClient.post()
//                    .uri(config.getOauthTokenUrl())
//                    .bodyValue(formData)
//                    .retrieve()
//                    .bodyToMono(Map.class)
//                    .block();
//
//            String accessToken = (String) tokenResponse.get("access_token");
//
//            // Get user info from IdP
//            Map<String, Object> userInfo = webClient.get()
//                    .uri(config.getIdpEntityId() + "/userinfo")
//                    .header("Authorization", "Bearer " + accessToken)
//                    .retrieve()
//                    .bodyToMono(Map.class)
//                    .block();
//
//            String email = (String) userInfo.get("email");
//
//            // Get user
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
//            log.error("OAuth SSO authentication failed", e);
//            throw new SSOAuthenticationException("OAuth SSO authentication failed: " + e.getMessage());
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