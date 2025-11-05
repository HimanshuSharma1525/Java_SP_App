//// SamlSSOService.java
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
//import org.w3c.dom.Document;
//import org.w3c.dom.NodeList;
//import javax.xml.parsers.DocumentBuilder;
//import javax.xml.parsers.DocumentBuilderFactory;
//import java.io.ByteArrayInputStream;
//import java.util.Base64;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class SamlSSOService {
//
//    private final SSOConfigRepository ssoConfigRepository;
//    private final UserRepository userRepository;
//    private final TenantService tenantService;
//    private final JwtTokenProvider jwtTokenProvider;
//
//    public String getAuthorizationUrl() {
//        Tenant tenant = tenantService.getCurrentTenant();
//        SSOConfig config = ssoConfigRepository.findByTenantAndProvider(tenant, SSOProvider.SAML)
//                .orElseThrow(() -> new SSOAuthenticationException("SAML SSO not configured"));
//
//        if (!config.getEnabled()) {
//            throw new SSOAuthenticationException("SAML SSO is not enabled");
//        }
//
//        // Build SAML AuthnRequest and redirect to IdP
//        String samlRequest = buildSamlRequest(config);
//        String encodedRequest = Base64.getEncoder().encodeToString(samlRequest.getBytes());
//
//        return config.getSamlSsoUrl() + "?SAMLRequest=" + encodedRequest;
//    }
//
//    public LoginResponse authenticate(String samlResponse) {
//        Tenant tenant = tenantService.getCurrentTenant();
//        SSOConfig config = ssoConfigRepository.findByTenantAndProvider(tenant, SSOProvider.SAML)
//                .orElseThrow(() -> new SSOAuthenticationException("SAML SSO not configured"));
//
//        if (!config.getEnabled()) {
//            throw new SSOAuthenticationException("SAML SSO is not enabled");
//        }
//
//        try {
//            // Decode and parse SAML response
//            String decodedResponse = new String(Base64.getDecoder().decode(samlResponse));
//            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//            DocumentBuilder builder = factory.newDocumentBuilder();
//            Document doc = builder.parse(new ByteArrayInputStream(decodedResponse.getBytes()));
//
//            // Validate signature using certificate (simplified - use proper SAML library in production)
//            // Extract email from SAML assertion
//            NodeList emailNodes = doc.getElementsByTagName("EmailAddress");
//            if (emailNodes.getLength() == 0) {
//                throw new SSOAuthenticationException("Email not found in SAML response");
//            }
//
//            String email = emailNodes.item(0).getTextContent();
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
//            log.error("SAML SSO authentication failed", e);
//            throw new SSOAuthenticationException("SAML SSO authentication failed: " + e.getMessage());
//        }
//    }
//
//    private String buildSamlRequest(SSOConfig config) {
//        // Simplified SAML AuthnRequest (use proper SAML library in production)
//        return String.format("""
//                <samlp:AuthnRequest xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol"
//                    ID="_%s"
//                    Version="2.0"
//                    IssueInstant="%s"
//                    Destination="%s"
//                    AssertionConsumerServiceURL="%s">
//                    <saml:Issuer xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion">%s</saml:Issuer>
//                </samlp:AuthnRequest>
//                """,
//                java.util.UUID.randomUUID(),
//                java.time.Instant.now(),
//                config.getSamlSsoUrl(),
//                config.getSamlAcsUrl(),
//                config.getSamlSpEntityId()
//        );
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
