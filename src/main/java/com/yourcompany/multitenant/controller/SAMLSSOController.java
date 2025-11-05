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
import java.util.Base64;
import java.util.Collections;
import java.util.Optional;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

@Slf4j
@Controller
@RequestMapping("/sso/saml")
@RequiredArgsConstructor
public class SAMLSSOController {

    private final SSOConfigRepository ssoConfigRepository;
    private final UserRepository userRepository;
    private final TenantService tenantService;

    @GetMapping("/login")
    public String samlLogin(HttpServletRequest request) {
        try {
            Tenant tenant = tenantService.getCurrentTenant();
            Optional<SSOConfig> configOpt = ssoConfigRepository.findByTenantAndProvider(tenant, SSOProvider.SAML);

            if (configOpt.isEmpty() || !configOpt.get().getEnabled() || configOpt.get().getSamlSsoUrl() == null) {
                return "redirect:/login.html?error=saml_not_configured";
            }

            SSOConfig config = configOpt.get();
            String baseUrl = request.getRequestURL().toString().replace(request.getRequestURI(), request.getContextPath());
            String acsUrl = config.getSamlAcsUrl() != null ? config.getSamlAcsUrl() : baseUrl + "/sso/saml/callback";
            String issuer = config.getSamlSpEntityId() != null ? config.getSamlSpEntityId() : baseUrl + "/sso/saml/metadata";

            String authnRequest = String.format("""
                <samlp:AuthnRequest xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol"
                    ID="_12345"
                    Version="2.0"
                    IssueInstant="2025-10-31T12:00:00Z"
                    ProtocolBinding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST"
                    AssertionConsumerServiceURL="%s">
                    <saml:Issuer xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion">%s</saml:Issuer>
                    <samlp:NameIDPolicy AllowCreate="true"
                        Format="urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress"/>
                </samlp:AuthnRequest>
                """, acsUrl, issuer);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            Deflater deflater = new Deflater(Deflater.DEFLATED, true);
            DeflaterOutputStream deflaterStream = new DeflaterOutputStream(byteArrayOutputStream, deflater);
            deflaterStream.write(authnRequest.getBytes(StandardCharsets.UTF_8));
            deflaterStream.close();

            String samlRequest = Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
            String encodedRequest = URLEncoder.encode(samlRequest, StandardCharsets.UTF_8);

            return "redirect:" + config.getSamlSsoUrl() + "?SAMLRequest=" + encodedRequest;

        } catch (Exception e) {
            log.error("Error initiating SAML login", e);
            return "redirect:/login.html?error=saml_error";
        }
    }

    @PostMapping("/callback")
    public String samlCallback(@RequestParam(value = "SAMLResponse", required = false) String samlResponse,
                               HttpServletRequest request) {
        try {
            if (samlResponse == null || samlResponse.isEmpty()) {
                return "redirect:/login.html?error=no_saml_response";
            }

            Tenant tenant = tenantService.getCurrentTenant();

            byte[] decodedBytes = Base64.getDecoder().decode(samlResponse);
            String xml = new String(decodedBytes);

            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new java.io.ByteArrayInputStream(xml.getBytes()));
            document.getDocumentElement().normalize();

            String email = document.getElementsByTagName("saml:NameID").item(0).getTextContent();

            if (email == null || email.isEmpty()) {
                return "redirect:/login.html?error=invalid_saml_response";
            }

            // Create user if doesn't exist
            Optional<User> existingUser = userRepository.findByEmail(email);
            User user = existingUser.orElseGet(() -> {
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setPassword("SSO_USER");
                newUser.setFirstName("SAML User");
                newUser.setLastName("SAML User");
                newUser.setTenant(tenant);
                return userRepository.save(newUser);
            });

            // Set Spring Security context
            var auth = new UsernamePasswordAuthenticationToken(
                    user.getEmail(), null,
                    Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name()))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

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
        String acsUrl = baseUrl + "/sso/saml/callback";

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<EntityDescriptor xmlns=\"urn:oasis:names:tc:SAML:2.0:metadata\" entityID=\"" + entityId + "\">"
                + "<SPSSODescriptor WantAssertionsSigned=\"false\" AuthnRequestsSigned=\"false\" protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\">"
                + "<AssertionConsumerService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\" Location=\"" + acsUrl + "\" index=\"1\"/>"
                + "</SPSSODescriptor>"
                + "</EntityDescriptor>";
    }
}
