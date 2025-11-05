package com.yourcompany.multitenant.controller;

import com.yourcompany.multitenant.model.SSOConfig;
import com.yourcompany.multitenant.model.SSOProvider;
import com.yourcompany.multitenant.model.Tenant;
import com.yourcompany.multitenant.model.User;
import com.yourcompany.multitenant.repository.SSOConfigRepository;
import com.yourcompany.multitenant.repository.UserRepository;
import com.yourcompany.multitenant.service.TenantService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;

@Slf4j
@Controller
@RequestMapping("/sso/oauth")
@RequiredArgsConstructor
public class OAuthSSOController {

    private final SSOConfigRepository ssoConfigRepository;
    private final UserRepository userRepository;
    private final TenantService tenantService;

    // Step 1: Redirect to OAuth provider
    @GetMapping("/login")
    public String oauthLogin() {
        try {
            Tenant tenant = tenantService.getCurrentTenant();
            Optional<SSOConfig> configOpt = ssoConfigRepository.findByTenantAndProvider(tenant, SSOProvider.OAUTH);

            if (configOpt.isEmpty() || !configOpt.get().getEnabled()) {
                return "redirect:/login.html?error=oauth_not_configured";
            }

            SSOConfig config = configOpt.get();
            String authorizeUrl = config.getOauthAuthorizationUrl() + "?response_type=code"
                    + "&client_id=" + config.getOauthClientId()
                    + "&redirect_uri=" + URLEncoder.encode(config.getOauthRedirectUri(), StandardCharsets.UTF_8)
                    + "&scope=" + URLEncoder.encode(config.getOauthScopes() != null ? config.getOauthScopes() : "openid profile email", StandardCharsets.UTF_8);

            return "redirect:" + authorizeUrl;
        } catch (Exception e) {
            log.error("Error initiating OAuth login", e);
            return "redirect:/login.html?error=oauth_error";
        }
    }

    // Step 2: Handle OAuth callback
    @GetMapping("/callback")
    public String oauthCallback(@RequestParam(required = false) String code,
                                @RequestParam(required = false) String error,
                                HttpServletRequest request) {
        if (error != null) {
            return "redirect:/login.html?error=oauth_" + error;
        }

        try {
            Tenant tenant = tenantService.getCurrentTenant();
            Optional<SSOConfig> configOpt = ssoConfigRepository.findByTenantAndProvider(tenant, SSOProvider.OAUTH);

            if (configOpt.isEmpty()) {
                return "redirect:/login.html?error=oauth_config_not_found";
            }

            SSOConfig config = configOpt.get();
            RestTemplate restTemplate = new RestTemplate();

            // Exchange code for access token
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            String body = "grant_type=authorization_code"
                    + "&code=" + code
                    + "&redirect_uri=" + URLEncoder.encode(config.getOauthRedirectUri(), StandardCharsets.UTF_8)
                    + "&client_id=" + config.getOauthClientId()
                    + "&client_secret=" + config.getOauthClientSecret();

            HttpEntity<String> tokenRequest = new HttpEntity<>(body, headers);
            ResponseEntity<String> tokenResponse = restTemplate.exchange(
                    config.getOauthTokenUrl(),
                    HttpMethod.POST,
                    tokenRequest,
                    String.class
            );

            JSONObject tokenJson = new JSONObject(tokenResponse.getBody());
            String accessToken = tokenJson.getString("access_token");

            // Fetch user info
            HttpHeaders userHeaders = new HttpHeaders();
            userHeaders.setBearerAuth(accessToken);
            HttpEntity<Void> userRequest = new HttpEntity<>(userHeaders);

            String userInfoUrl = config.getOauthUserInfoUrl() != null ?
                    config.getOauthUserInfoUrl() :
                    (config.getIdpEntityId() != null ? config.getIdpEntityId() + "/userinfo" : null);

            ResponseEntity<String> userResponse = restTemplate.exchange(
                    userInfoUrl,
                    HttpMethod.GET,
                    userRequest,
                    String.class
            );

            JSONObject userInfo = new JSONObject(userResponse.getBody());
            String email = userInfo.optString("email");
            String firstName = userInfo.optString("given_name", "");
            String lastName = userInfo.optString("family_name", "");
            String name = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();

            if (email == null || email.isEmpty()) {
                return "redirect:/login.html?error=no_email";
            }

            // Create user if doesn't exist
            Optional<User> existingUser = userRepository.findByEmail(email);
            User user = existingUser.orElseGet(() -> {
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setPassword("SSO_USER");
                newUser.setFirstName(firstName.isEmpty() ? "OAuth User" : firstName);
                newUser.setLastName(lastName.isEmpty() ? "OAuth User" : lastName);
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

            // Redirect to dashboard based on role
            String dashboardUrl = switch (user.getRole()) {
                case SUPER_ADMIN -> "/super-admin-dashboard.html";
                case CUSTOMER_ADMIN -> "/customer-admin-dashboard.html";
                case END_USER -> "/end-user-dashboard.html";
            };

            return "redirect:" + dashboardUrl;

        } catch (Exception e) {
            log.error("OAuth callback error", e);
            return "redirect:/login.html?error=oauth_auth_failed";
        }
    }
}
