// SSOConfig.java
package com.yourcompany.multitenant.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "ssoconfig", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tenant_id", "provider"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SSOConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SSOProvider provider;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = false;

    // Common fields
    @Column(name = "idp_entity_id")
    private String idpEntityId;

    // JWT specific fields
    @Column(name = "jwt_secret")
    private String jwtSecret;

    @Column(name = "jwt_certificate")
    private String jwtCertificate;

    @Column(name = "jwt_sso_url")
    private String jwtUrl;

    @Column(name = "jwt_issuer")
    private String jwtIssuer;

    // SAML specific fields
    @Column(name = "saml_sso_url", length = 512)
    private String samlSsoUrl;

    @Column(name = "saml_certificate", length = 4096)
    private String samlCertificate;

    @Column(name = "saml_sp_entity_id")
    private String samlSpEntityId;

    @Column(name = "saml_acs_url")
    private String samlAcsUrl;

    // OAuth specific fields
    @Column(name = "oauth_client_id")
    private String oauthClientId;

    @Column(name = "oauth_client_secret")
    private String oauthClientSecret;

    @Column(name = "oauth_authorization_url")
    private String oauthAuthorizationUrl;

    @Column(name = "oauth_token_url")
    private String oauthTokenUrl;

    @Column(name = "oauthUser_Info_Url")
    private String oauthUserInfoUrl;

    @Column(name = "oauth_redirect_uri")
    private String oauthRedirectUri;

    @Column(name = "oauth_scopes")
    private String oauthScopes; // Comma-separated

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ✅ Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public SSOProvider getProvider() { return provider; }
    public void setProvider(SSOProvider provider) { this.provider = provider; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public String getIdpEntityId() { return idpEntityId; }
    public void setIdpEntityId(String idpEntityId) { this.idpEntityId = idpEntityId; }

    public String getJwtSecret() { return jwtSecret; }
    public void setJwtSecret(String jwtSecret) { this.jwtSecret = jwtSecret; }

    public String getJwtTokenEndpoint() { return jwtCertificate; }
    public void setJwtTokenEndpoint(String jwtTokenEndpoint) { this.jwtCertificate = jwtCertificate; }

    public String getJwtUrl() { return jwtUrl; }
    public void setJwtUrl(String jwtUrl) { this.jwtUrl = jwtUrl; }

    public String getJwtIssuer() { return jwtIssuer; }
    public void setJwtIssuer(String jwtIssuer) { this.jwtIssuer = jwtIssuer; }

    public String getSamlSsoUrl() { return samlSsoUrl; }
    public void setSamlSsoUrl(String samlSsoUrl) { this.samlSsoUrl = samlSsoUrl; }

    public String getSamlCertificate() { return samlCertificate; }
    public void setSamlCertificate(String samlCertificate) { this.samlCertificate = samlCertificate; }

    public String getSamlSpEntityId() { return samlSpEntityId; }
    public void setSamlSpEntityId(String samlSpEntityId) { this.samlSpEntityId = samlSpEntityId; }

    public String getSamlAcsUrl() { return samlAcsUrl; }
    public void setSamlAcsUrl(String samlAcsUrl) { this.samlAcsUrl = samlAcsUrl; }

    public String getOauthClientId() { return oauthClientId; }
    public void setOauthClientId(String oauthClientId) { this.oauthClientId = oauthClientId; }

    public String getOauthClientSecret() { return oauthClientSecret; }
    public void setOauthClientSecret(String oauthClientSecret) { this.oauthClientSecret = oauthClientSecret; }

    public String getOauthAuthorizationUrl() { return oauthAuthorizationUrl; }
    public void setOauthAuthorizationUrl(String oauthAuthorizationUrl) { this.oauthAuthorizationUrl = oauthAuthorizationUrl; }

    public String getOauthTokenUrl() { return oauthTokenUrl; }
    public void setOauthTokenUrl(String oauthTokenUrl) { this.oauthTokenUrl = oauthTokenUrl; }

    public String getOauthRedirectUri() { return oauthRedirectUri; }
    public void setOauthRedirectUri(String oauthRedirectUri) { this.oauthRedirectUri = oauthRedirectUri; }

    public String getOauthScopes() { return oauthScopes; }
    public void setOauthScopes(String oauthScopes) { this.oauthScopes = oauthScopes; }

    public String getOauthUserInfoUrl() { return oauthUserInfoUrl; }
    public void setOauthUserInfoUrl(String oauthUserInfoUrl) { this.oauthUserInfoUrl = oauthUserInfoUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}