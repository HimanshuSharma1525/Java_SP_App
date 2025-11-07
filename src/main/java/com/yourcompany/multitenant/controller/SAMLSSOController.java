package com.yourcompany.multitenant.controller;

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
import org.springframework.web.bind.annotation.*;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

@Slf4j
@Controller
@RequestMapping("/sso/saml")
@RequiredArgsConstructor
public class SAMLSSOController {

    private final TenantService tenantService;
    private final SSOConfigRepository ssoConfigRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/login")
    public String samlLogin(HttpServletRequest request) {
        try {
            final Tenant tenant = tenantService.getCurrentTenant();
            Optional<SSOConfig> cfgOpt = ssoConfigRepository.findByTenantAndProvider(tenant, SSOProvider.SAML);

            if (cfgOpt.isEmpty() || !Boolean.TRUE.equals(cfgOpt.get().getEnabled()) || cfgOpt.get().getSamlSsoUrl() == null) {
                return "redirect:/login.html?error=saml_not_configured";
            }

            SSOConfig cfg = cfgOpt.get();

            String baseUrl = request.getRequestURL().toString().replace(request.getRequestURI(), request.getContextPath());
            String acsUrl = (cfg.getSamlAcsUrl() == null || cfg.getSamlAcsUrl().isBlank())
                    ? baseUrl + "/sso/saml/callback"
                    : cfg.getSamlAcsUrl();
            String issuer = (cfg.getSamlSpEntityId() == null || cfg.getSamlSpEntityId().isBlank())
                    ? baseUrl + "/sso/saml/metadata"
                    : cfg.getSamlSpEntityId();

            String authnRequest = """
                <samlp:AuthnRequest xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol"
                    ID="_%s"
                    Version="2.0"
                    IssueInstant="%s"
                    ProtocolBinding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST"
                    AssertionConsumerServiceURL="%s">
                    <saml:Issuer xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion">%s</saml:Issuer>
                    <samlp:NameIDPolicy AllowCreate="true"
                        Format="urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress"/>
                </samlp:AuthnRequest>
                """.formatted(UUID.randomUUID(), java.time.Instant.now().toString(), acsUrl, issuer);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (DeflaterOutputStream ds = new DeflaterOutputStream(out, new Deflater(Deflater.DEFLATED, true))) {
                ds.write(authnRequest.getBytes(StandardCharsets.UTF_8));
            }
            String samlRequest = Base64.getEncoder().encodeToString(out.toByteArray());
            String encoded = URLEncoder.encode(samlRequest, StandardCharsets.UTF_8);

            return "redirect:" + cfg.getSamlSsoUrl() + "?SAMLRequest=" + encoded;

        } catch (Exception e) {
            log.error("Error initiating SAML login", e);
            return "redirect:/login.html?error=saml_error";
        }
    }

    @PostMapping("/callback")
    public String samlCallback(@RequestParam(value = "SAMLResponse", required = false) String samlResponse,
                               HttpServletRequest request) {
        try {
            if (samlResponse == null || samlResponse.isBlank()) {
                return "redirect:/login.html?error=no_saml_response";
            }

            final Tenant tenant = tenantService.getCurrentTenant();

            byte[] decoded = Base64.getDecoder().decode(samlResponse);
            String xml = new String(decoded, StandardCharsets.UTF_8);

            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new java.io.ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            document.getDocumentElement().normalize();

            String email;
            if (document.getElementsByTagName("saml:NameID").getLength() > 0) {
                email = document.getElementsByTagName("saml:NameID").item(0).getTextContent();
            } else if (document.getElementsByTagName("NameID").getLength() > 0) {
                email = document.getElementsByTagName("NameID").item(0).getTextContent();
            } else {
                email = null;
            }
            if (email == null || email.isBlank()) {
                return "redirect:/login.html?error=invalid_saml_response";
            }

            final User user = userRepository.findByEmailAndTenant(email, tenant).orElseGet(() -> {
                User u = User.builder()
                        .email(email)
                        .firstName("SSO")
                        .lastName("User")
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

            return "redirect:/login.html?token=" + URLEncoder.encode(appToken, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("SAML callback error", e);
            return "redirect:/login.html?error=saml_auth_failed";
        }
    }

    @GetMapping(value = "/metadata", produces = "application/xml")
    @ResponseBody
    public String metadata(HttpServletRequest request) {
        String baseUrl = request.getRequestURL().toString().replace(request.getRequestURI(), request.getContextPath());
        String entityId = baseUrl + "/sso/saml/metadata";
        String acsUrl   = baseUrl + "/sso/saml/callback";

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<EntityDescriptor xmlns=\"urn:oasis:names:tc:SAML:2.0:metadata\" entityID=\"" + entityId + "\">"
                + "<SPSSODescriptor WantAssertionsSigned=\"false\" AuthnRequestsSigned=\"false\" protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\">"
                + "<AssertionConsumerService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\" Location=\"" + acsUrl + "\" index=\"1\"/>"
                + "</SPSSODescriptor>"
                + "</EntityDescriptor>";
    }
}
