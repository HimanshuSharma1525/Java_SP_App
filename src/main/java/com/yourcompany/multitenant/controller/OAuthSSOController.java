package com.yourcompany.multitenant.controller;

import com.yourcompany.multitenant.model.*;
import com.yourcompany.multitenant.repository.SSOConfigRepository;
import com.yourcompany.multitenant.repository.UserRepository;
import com.yourcompany.multitenant.security.JwtTokenProvider;
import com.yourcompany.multitenant.service.TenantService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Slf4j
@Controller
@RequestMapping("/sso/oauth")
@RequiredArgsConstructor
public class OAuthSSOController {

    private final TenantService tenantService;
    private final SSOConfigRepository ssoConfigRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/login")
    public String oauthLogin() {
        final Tenant tenant = tenantService.getCurrentTenant();
        Optional<SSOConfig> cfgOpt = ssoConfigRepository.findByTenantAndProvider(tenant, SSOProvider.OAUTH);

        if (cfgOpt.isEmpty() || !Boolean.TRUE.equals(cfgOpt.get().getEnabled())) {
            return "redirect:/login.html?error=oauth_not_configured";
        }

        SSOConfig cfg = cfgOpt.get();

        String authUrl = cfg.getOauthAuthorizationUrl()
                + "?response_type=code"
                + "&client_id=" + url(cfg.getOauthClientId())
                + "&redirect_uri=" + url(cfg.getOauthRedirectUri())
                + "&scope=" + url(cfg.getOauthScopes() == null ? "openid profile email" : cfg.getOauthScopes());

        return "redirect:" + authUrl;
    }

    @GetMapping("/callback")
    public String oauthCallback(String code, String error) {
        try {
            if (error != null) {
                return "redirect:/login.html?error=oauth_failed";
            }
            if (code == null || code.isBlank()) {
                return "redirect:/login.html?error=missing_code";
            }

            final Tenant tenant = tenantService.getCurrentTenant();

            final SSOConfig cfg = ssoConfigRepository.findByTenantAndProvider(tenant, SSOProvider.OAUTH)
                    .orElse(null);

            if (cfg == null || !Boolean.TRUE.equals(cfg.getEnabled())) {
                return "redirect:/login.html?error=oauth_not_configured";
            }

            RestTemplate rt = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String body = "grant_type=authorization_code"
                    + "&code=" + url(code)
                    + "&redirect_uri=" + url(cfg.getOauthRedirectUri())
                    + "&client_id=" + url(cfg.getOauthClientId())
                    + "&client_secret=" + url(cfg.getOauthClientSecret());

            ResponseEntity<String> tokenResp = rt.exchange(
                    cfg.getOauthTokenUrl(), HttpMethod.POST, new HttpEntity<>(body, headers), String.class
            );

            if (!tokenResp.getStatusCode().is2xxSuccessful()) {
                return "redirect:/login.html?error=oauth_failed";
            }

            JSONObject tokenJson = new JSONObject(tokenResp.getBody());
            String accessToken = tokenJson.optString("access_token", null);
            if (accessToken == null) {
                return "redirect:/login.html?error=oauth_failed";
            }

            HttpHeaders uheaders = new HttpHeaders();
            uheaders.setBearerAuth(accessToken);
            ResponseEntity<String> userResp = rt.exchange(
                    cfg.getOauthUserInfoUrl(), HttpMethod.GET, new HttpEntity<>(uheaders), String.class
            );
            if (!userResp.getStatusCode().is2xxSuccessful()) {
                return "redirect:/login.html?error=oauth_failed";
            }

            JSONObject userInfo = new JSONObject(userResp.getBody());
            String email = firstNonBlank(
                    userInfo.optString("email", null),
                    userInfo.optString("upn", null),
                    userInfo.optString("preferred_username", null)
            );
            if (email == null || email.isBlank()) {
                return "redirect:/login.html?error=oauth_no_email";
            }

            String given = userInfo.optString("given_name", "");
            String family = userInfo.optString("family_name", "");
            if (given.isBlank() && family.isBlank()) {
                String name = userInfo.optString("name", "");
                if (!name.isBlank()) {
                    String[] parts = name.trim().split("\\s+", 2);
                    given = parts[0];
                    if (parts.length > 1) family = parts[1];
                }
            }

            String finalGiven = given;
            String finalFamily = family;
            final User user = userRepository.findByEmailAndTenant(email, tenant).orElseGet(() -> {
                User u = User.builder()
                        .email(email)
                        .firstName(finalGiven.isBlank() ? "SSO" : finalGiven)
                        .lastName(finalFamily.isBlank() ? "User" : finalFamily)
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

            String redirectUrl = switch (user.getRole()) {
                case SUPER_ADMIN -> "/super-admin-dashboard.html";
                case CUSTOMER_ADMIN -> "/customer-admin-dashboard.html";
                case END_USER -> "/end-user-dashboard.html";
            };

            return "redirect:/login.html?token=" + url(appToken);


        } catch (Exception e) {
            log.error("OAuth callback error", e);
            return "redirect:/login.html?error=oauth_failed";
        }
    }

    private static String url(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
    private static String firstNonBlank(String... arr) {
        for (String s : arr) if (s != null && !s.isBlank()) return s;
        return null;
    }
}
