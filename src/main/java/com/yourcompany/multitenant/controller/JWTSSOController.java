package com.yourcompany.multitenant.controller;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import com.yourcompany.multitenant.model.*;
import com.yourcompany.multitenant.repository.SSOConfigRepository;
import com.yourcompany.multitenant.repository.UserRepository;
import com.yourcompany.multitenant.security.JwtTokenProvider;
import com.yourcompany.multitenant.service.TenantService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;

@Slf4j
@Controller
@RequestMapping("/sso/jwt")
@RequiredArgsConstructor
public class JWTSSOController {

    private final TenantService tenantService;
    private final SSOConfigRepository ssoConfigRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/login")
    public String redirectToJWT() {
        final Tenant tenant = tenantService.getCurrentTenant();
        Optional<SSOConfig> cfgOpt = ssoConfigRepository.findByTenantAndProvider(tenant, SSOProvider.JWT);

        if (cfgOpt.isEmpty() || !Boolean.TRUE.equals(cfgOpt.get().getEnabled()) || cfgOpt.get().getJwtUrl() == null) {
            return "redirect:/login.html?error=jwt_not_configured";
        }

        return "redirect:" + cfgOpt.get().getJwtUrl();
    }

    @GetMapping({"/callback", "/callback/**", "/callback*"})
    public String jwtCallback(HttpServletRequest request) {
        try {
            final Tenant tenant = tenantService.getCurrentTenant();
            final SSOConfig cfg = ssoConfigRepository.findByTenantAndProvider(tenant, SSOProvider.JWT)
                    .orElse(null);

            if (cfg == null || !Boolean.TRUE.equals(cfg.getEnabled())) {
                return "redirect:/login.html?error=jwt_disabled";
            }

            String idToken = Optional.ofNullable(request.getParameter("id_token")).orElseGet(() -> {
                String uri = request.getRequestURI();
                String prefix = "/sso/jwt/callback";
                if (uri.contains(prefix)) {
                    String tail = uri.substring(uri.indexOf(prefix) + prefix.length());
                    if (tail.startsWith("/")) tail = tail.substring(1);
                    return tail;
                }
                return null;
            });

            if (idToken == null || idToken.isBlank()) {
                return "redirect:/login.html?error=missing_token";
            }

            SignedJWT signed = SignedJWT.parse(idToken);
            String alg = signed.getHeader().getAlgorithm().getName();

            boolean verified;
            if ("RS256".equalsIgnoreCase(alg) && cfg.getJwtCertificate() != null && !cfg.getJwtCertificate().isBlank()) {
                verified = verifyRS256WithX509(signed, cfg.getJwtCertificate());
            } else {
                verified = verifyHS256(signed, cfg.getJwtSecret());
            }

            if (!verified) {
                return "redirect:/login.html?error=invalid_signature";
            }

            var claims = signed.getJWTClaimsSet();
            String email = Optional.ofNullable(claims.getStringClaim("email")).orElse(claims.getSubject());

            if (email == null || email.isBlank()) {
                return "redirect:/login.html?error=invalid_token";
            }

            String firstName = optString(claims.getStringClaim("first_name"));
            String lastName  = optString(claims.getStringClaim("last_name"));

            final User user = userRepository.findByEmailAndTenant(email, tenant).orElseGet(() -> {
                User u = User.builder()
                        .email(email)
                        .firstName(firstName.isBlank() ? "SSO" : firstName)
                        .lastName(lastName.isBlank() ? "User" : lastName)
                        .password("{noop}SSO_USER")
                        .role(Role.END_USER)
                        .active(true)
                        .tenant(tenant)
                        .build();
                return userRepository.save(u);
            });

            String appToken = jwtTokenProvider.generateToken(
                    user.getId(), user.getEmail(), user.getRole(), tenant.getId()
            );

            var auth = new UsernamePasswordAuthenticationToken(
                    user.getEmail(), null, List.of(new SimpleGrantedAuthority(user.getRole().name()))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            // ✅ Change here: redirect to login.html with token, not directly to dashboard
            return "redirect:/login.html?token=" + URLEncoder.encode(appToken, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("JWT SSO callback error", e);
            return "redirect:/login.html?error=jwt_failed";
        }
    }


    private boolean verifyHS256(SignedJWT jwt, String secret) throws JOSEException {
        if (secret == null || secret.isBlank()) return false;
        JWSVerifier verifier = new MACVerifier(secret.getBytes(StandardCharsets.UTF_8));
        return jwt.verify(verifier);
    }

    private boolean verifyRS256WithX509(SignedJWT jwt, String certificatePEM) {
        try {
            String pem = certificatePEM.replace("\\n", "\n").trim();
            String cleaned = pem.replace("-----BEGIN CERTIFICATE-----", "")
                    .replace("-----END CERTIFICATE-----", "")
                    .replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(cleaned);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(new java.io.ByteArrayInputStream(decoded));
            RSAPublicKey publicKey = (RSAPublicKey) cert.getPublicKey();
            JWSVerifier verifier = new RSASSAVerifier(publicKey);
            return jwt.verify(verifier);
        } catch (Exception e) {
            log.error("RS256 X509 verification failed", e);
            return false;
        }
    }

    private static String optString(String s) { return s == null ? "" : s; }
}
