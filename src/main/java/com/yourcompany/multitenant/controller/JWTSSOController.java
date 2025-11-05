// JWTSSOController.java
package com.yourcompany.multitenant.controller;

import com.yourcompany.multitenant.model.SSOConfig;
import com.yourcompany.multitenant.model.SSOProvider;
import com.yourcompany.multitenant.model.Tenant;
import com.yourcompany.multitenant.model.User;
import com.yourcompany.multitenant.repository.SSOConfigRepository;
import com.yourcompany.multitenant.repository.UserRepository;
import com.yourcompany.multitenant.service.TenantService;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Collections;
import java.util.Optional;

@Slf4j
@Controller
@RequestMapping("/sso/jwt")
@RequiredArgsConstructor
public class JWTSSOController {

    private final UserRepository userRepository;
    private final SSOConfigRepository ssoConfigRepository;
    private final TenantService tenantService;

    // Step 1: Redirect user to miniOrange JWT App login
    @GetMapping("/login")
    public String redirectToSSO() {
        try {
            Tenant tenant = tenantService.getCurrentTenant();
            Optional<SSOConfig> configOpt = ssoConfigRepository.findByTenantAndProvider(tenant, SSOProvider.JWT);

            if (configOpt.isEmpty() || !configOpt.get().getEnabled() || configOpt.get().getJwtTokenEndpoint() == null) {
                return "redirect:/login.html?error=jwt_not_configured";
            }

            return "redirect:" + configOpt.get().getJwtTokenEndpoint();
        } catch (Exception e) {
            log.error("Error redirecting to JWT SSO", e);
            return "redirect:/login.html?error=jwt_error";
        }
    }

    // Step 2: Handle JWT callback from miniOrange
    @GetMapping({"/callback", "/callback/**", "/callback*"})
    public String handleSSOCallback(HttpServletRequest request) throws Exception {
        Tenant tenant = tenantService.getCurrentTenant();
        Optional<SSOConfig> configOpt = ssoConfigRepository.findByTenantAndProvider(tenant, SSOProvider.JWT);

        if (configOpt.isEmpty() || !configOpt.get().getEnabled()) {
            return "redirect:/login.html?error=jwt_disabled";
        }

        SSOConfig config = configOpt.get();
        String jwtSecret = config.getJwtSecret();
        String publicKeyPEM = config.getJwtCertificate();

        String idToken = request.getParameter("id_token");

        // Extract token from URL if not in query param
        if (idToken == null || idToken.isEmpty()) {
            String requestURI = request.getRequestURI();
            if (requestURI.contains("/sso/jwt/callback")) {
                idToken = requestURI.substring(requestURI.indexOf("/sso/jwt/callback") + "/sso/jwt/callback".length());
                if (idToken.startsWith("/")) idToken = idToken.substring(1);
            }
        }

        if (idToken == null || idToken.isEmpty()) return "redirect:/login.html?error=missing_token";

        SignedJWT signedJWT = SignedJWT.parse(idToken);
        String alg = signedJWT.getHeader().getAlgorithm().getName();

        boolean verified;
        if ("RS256".equalsIgnoreCase(alg) && publicKeyPEM != null && !publicKeyPEM.isBlank()) {
            verified = verifyRS256(signedJWT, publicKeyPEM);
        } else {
            verified = verifyHS256(signedJWT, jwtSecret);
        }

        if (!verified) return "redirect:/login.html?error=invalid_signature";

        var claims = signedJWT.getJWTClaimsSet();
        String email = claims.getStringClaim("email");
        String firstName = claims.getStringClaim("first_name");
        String lastName = claims.getStringClaim("last_name");
        String name = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();

        if (email == null || email.isBlank()) return "redirect:/login.html?error=invalid_token";

        // Create user iff doesn’t exist
        Optional<User> existingUser = userRepository.findByEmail(email);
        User user = existingUser.orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setPassword("SSO_USER");
            newUser.setFirstName(firstName.isEmpty() ? "SSO User" : name);
            newUser.setLastName(lastName.isEmpty() ? "SSO User" : lastName);
            newUser.setTenant(tenant);
            return userRepository.save(newUser);
        });

        // Set Spring Security context
        var auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(), null,
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name()))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Store info in session
        request.getSession().setAttribute("userEmail", user.getEmail());
        request.getSession().setAttribute("userRole", user.getRole().name());
        request.getSession().setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

        // Redirect based on role
        String dashboardUrl = switch (user.getRole()) {
            case SUPER_ADMIN -> "/super-admin-dashboard.html";
            case CUSTOMER_ADMIN -> "/customer-admin-dashboard.html";
            case END_USER -> "/end-user-dashboard.html";
        };

        return "redirect:" + dashboardUrl;
    }

    // Verify RS256 using PEM-formatted public key
    private boolean verifyRS256(SignedJWT signedJWT, String certificatePEM) {
        try {
            certificatePEM = certificatePEM.replace("\\n", "\n").replace("\r", "").trim();
            String cleanedPem = certificatePEM
                    .replace("-----BEGIN CERTIFICATE-----", "")
                    .replace("-----END CERTIFICATE-----", "")
                    .replaceAll("\\s+", "");
            byte[] decoded = java.util.Base64.getDecoder().decode(cleanedPem);
            java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
            java.io.ByteArrayInputStream inputStream = new java.io.ByteArrayInputStream(decoded);
            java.security.cert.X509Certificate cert = (java.security.cert.X509Certificate) cf.generateCertificate(inputStream);

            JWSVerifier verifier = new RSASSAVerifier((java.security.interfaces.RSAPublicKey) cert.getPublicKey());
            return signedJWT.verify(verifier);
        } catch (Exception e) {
            log.error("❌ Failed to verify RS256 token", e);
            return false;
        }
    }

    // Verify HS256 using shared secret
    private boolean verifyHS256(SignedJWT signedJWT, String secret) {
        try {
            if (secret == null || secret.isBlank()) {
                log.error("❌ HS256 secret missing!");
                return false;
            }
            JWSVerifier verifier = new MACVerifier(secret.getBytes());
            return signedJWT.verify(verifier);
        } catch (JOSEException e) {
            log.error("❌ Failed to verify HS256 token", e);
            return false;
        }
    }
}
