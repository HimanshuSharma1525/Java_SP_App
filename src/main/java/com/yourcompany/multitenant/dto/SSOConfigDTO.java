package com.yourcompany.multitenant.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SSOConfigDTO {
    private Long id;
    private String provider;
    private Boolean enabled;

    // 🔹 JWT Fields
    private String jwtUrl;
    private String jwtSecret;
    private String jwtIssuer;
    private String jwtCertificate;

    // 🔹 SAML Fields
    private String idpEntityId;
    private String samlSsoUrl;
    private String samlCertificate;
    private String samlSpEntityId;
    private String samlAcsUrl;

    // 🔹 OAuth Fields
    private String oauthClientId;
    private String oauthClientSecret;
    private String oauthAuthorizationUrl;
    private String oauthTokenUrl;
    private String oauthUserInfoUrl;
    private String oauthRedirectUri;
    private String oauthScopes;
}
