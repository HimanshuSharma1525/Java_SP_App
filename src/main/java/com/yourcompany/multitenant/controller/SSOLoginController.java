//package com.yourcompany.multitenant.controller;
//
//import com.yourcompany.multitenant.model.SSOConfig;
//import com.yourcompany.multitenant.model.SSOProvider;
//import com.yourcompany.multitenant.model.Tenant;
//import com.yourcompany.multitenant.repository.SSOConfigRepository;
//import com.yourcompany.multitenant.service.TenantService;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.io.IOException;
//
//@RestController
//@RequestMapping("/sso")
//@RequiredArgsConstructor
//public class SSOLoginController {
//
//    private final TenantService tenantService;
//    private final SSOConfigRepository ssoConfigRepository;
//
//    // ------------------ JWT LOGIN ------------------
//    @GetMapping("/jwt/login")
//    public void jwtLogin(HttpServletResponse response) throws IOException {
//        Tenant tenant = tenantService.getCurrentTenant();
//        SSOConfig config = ssoConfigRepository.findByTenantAndProvider(tenant, SSOProvider.JWT)
//                .orElseThrow(() -> new IllegalStateException("JWT SSO not configured for tenant"));
//
//        if (!Boolean.TRUE.equals(config.getEnabled())) {
//            response.sendError(HttpStatus.FORBIDDEN.value(), "JWT SSO is disabled for this tenant");
//            return;
//        }
//
//        String redirectUrl = sanitizeUrl(config.getJwtUrl());
//        response.sendRedirect(redirectUrl);
//    }
//
//    // ------------------ SAML LOGIN ------------------
//    @GetMapping("/saml/login")
//    public void samlLogin(HttpServletResponse response) throws IOException {
//        Tenant tenant = tenantService.getCurrentTenant();
//        SSOConfig config = ssoConfigRepository.findByTenantAndProvider(tenant, SSOProvider.SAML)
//                .orElseThrow(() -> new IllegalStateException("SAML SSO not configured for tenant"));
//
//        if (!Boolean.TRUE.equals(config.getEnabled())) {
//            response.sendError(HttpStatus.FORBIDDEN.value(), "SAML SSO is disabled for this tenant");
//            return;
//        }
//
//        String redirectUrl = sanitizeUrl(config.getSamlSsoUrl());
//        response.sendRedirect(redirectUrl);
//    }
//
//    // ------------------ OAUTH LOGIN ------------------
//    @GetMapping("/oauth/login")
//    public void oauthLogin(HttpServletResponse response) throws IOException {
//        Tenant tenant = tenantService.getCurrentTenant();
//        SSOConfig config = ssoConfigRepository.findByTenantAndProvider(tenant, SSOProvider.OAUTH)
//                .orElseThrow(() -> new IllegalStateException("OAuth SSO not configured for tenant"));
//
//        if (!Boolean.TRUE.equals(config.getEnabled())) {
//            response.sendError(HttpStatus.FORBIDDEN.value(), "OAuth SSO is disabled for this tenant");
//            return;
//        }
//
//        String redirectUrl = sanitizeUrl(config.getOauthAuthorizationUrl());
//        response.sendRedirect(redirectUrl);
//    }
//
//    // ------------------ HELPER: sanitize URL ------------------
//    private String sanitizeUrl(String url) {
//        if (url == null || url.isBlank()) {
//            throw new IllegalArgumentException("SSO URL is empty");
//        }
//        // Remove CR/LF and trim whitespace
//        url = url.replaceAll("[\\r\\n]", "").trim();
//
//        // Optionally: validate URL format here if needed
//        return url;
//    }
//
//    // ------------------ OPTIONAL: Test endpoint to check config ------------------
//    @GetMapping("/providers")
//    public ResponseEntity<?> listEnabledProviders() {
//        Tenant tenant = tenantService.getCurrentTenant();
//        return ResponseEntity.ok(ssoConfigRepository.findByTenant(tenant)
//                .stream()
//                .filter(SSOConfig::getEnabled)
//                .map(SSOConfig::getProvider)
//                .toList());
//    }
//}
