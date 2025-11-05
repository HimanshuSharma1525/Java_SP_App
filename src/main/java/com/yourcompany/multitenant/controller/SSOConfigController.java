package com.yourcompany.multitenant.controller;

import com.yourcompany.multitenant.dto.SSOConfigDTO;
import com.yourcompany.multitenant.model.SSOConfig;
import com.yourcompany.multitenant.model.SSOProvider;
import com.yourcompany.multitenant.model.Tenant;
import com.yourcompany.multitenant.repository.SSOConfigRepository;
import com.yourcompany.multitenant.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sso/config")
@RequiredArgsConstructor
public class SSOConfigController {

    private final SSOConfigRepository ssoConfigRepository;
    private final TenantService tenantService;

    // ------------------ ADMIN ENDPOINTS ------------------

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'CUSTOMER_ADMIN')")
    public ResponseEntity<List<SSOConfigDTO>> getSSOConfigs() {
        Tenant tenant = tenantService.getCurrentTenant();
        List<SSOConfig> configs = ssoConfigRepository.findByTenant(tenant);

        List<SSOConfigDTO> dtos = configs.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{provider}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'CUSTOMER_ADMIN')")
    public ResponseEntity<SSOConfigDTO> getSSOConfig(@PathVariable String provider) {
        Tenant tenant = tenantService.getCurrentTenant();
        SSOProvider ssoProvider = SSOProvider.valueOf(provider.toUpperCase());

        SSOConfig config = ssoConfigRepository.findByTenantAndProvider(tenant, ssoProvider)
                .orElseThrow(() -> new IllegalArgumentException("SSO config not found"));

        return ResponseEntity.ok(convertToDTO(config));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'CUSTOMER_ADMIN')")
    public ResponseEntity<SSOConfigDTO> createOrUpdateSSOConfig(@RequestBody SSOConfigDTO dto) {
        Tenant tenant = tenantService.getCurrentTenant();
        SSOProvider provider = SSOProvider.valueOf(dto.getProvider().toUpperCase());

        SSOConfig config = ssoConfigRepository.findByTenantAndProvider(tenant, provider)
                .orElse(SSOConfig.builder()
                        .tenant(tenant)
                        .provider(provider)
                        .build());

        config.setEnabled(dto.getEnabled());

        // Map fields based on provider
        switch (provider) {
            case JWT -> {
                config.setJwtSecret(dto.getJwtSecret());
                config.setJwtCertificate(dto.getJwtCertificate());
                config.setJwtUrl(dto.getJwtUrl());
                config.setJwtIssuer(dto.getJwtIssuer());
            }
            case SAML -> {
                config.setIdpEntityId(dto.getIdpEntityId());
                config.setSamlSsoUrl(dto.getSamlSsoUrl());
                config.setSamlCertificate(dto.getSamlCertificate());
                config.setSamlSpEntityId(dto.getSamlSpEntityId());
                config.setSamlAcsUrl(dto.getSamlAcsUrl());
            }
            case OAUTH -> {
                config.setIdpEntityId(dto.getIdpEntityId());
                config.setOauthClientId(dto.getOauthClientId());
                config.setOauthClientSecret(dto.getOauthClientSecret());
                config.setOauthAuthorizationUrl(dto.getOauthAuthorizationUrl());
                config.setOauthTokenUrl(dto.getOauthTokenUrl());
                config.setOauthUserInfoUrl(dto.getOauthUserInfoUrl());
                config.setOauthRedirectUri(dto.getOauthRedirectUri());
                config.setOauthScopes(dto.getOauthScopes());
            }
        }

        config = ssoConfigRepository.save(config);
        return ResponseEntity.ok(convertToDTO(config));
    }

    @DeleteMapping("/{provider}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'CUSTOMER_ADMIN')")
    public ResponseEntity<Void> deleteSSOConfig(@PathVariable String provider) {
        Tenant tenant = tenantService.getCurrentTenant();
        SSOProvider ssoProvider = SSOProvider.valueOf(provider.toUpperCase());

        SSOConfig config = ssoConfigRepository.findByTenantAndProvider(tenant, ssoProvider)
                .orElseThrow(() -> new IllegalArgumentException("SSO config not found"));

        ssoConfigRepository.delete(config);
        return ResponseEntity.noContent().build();
    }

    // Convert entity to DTO
    private SSOConfigDTO convertToDTO(SSOConfig config) {
        return SSOConfigDTO.builder()
                .id(config.getId())
                .provider(config.getProvider().name())
                .enabled(config.getEnabled())
                .jwtSecret(config.getJwtSecret())
                .jwtCertificate(config.getJwtCertificate())
                .jwtUrl(config.getJwtUrl())
                .jwtIssuer(config.getJwtIssuer())
                .samlSsoUrl(config.getSamlSsoUrl())
                .samlCertificate(config.getSamlCertificate())
                .samlSpEntityId(config.getSamlSpEntityId())
                .samlAcsUrl(config.getSamlAcsUrl())
                .oauthClientId(config.getOauthClientId())
                .oauthClientSecret(config.getOauthClientSecret())
                .oauthAuthorizationUrl(config.getOauthAuthorizationUrl())
                .oauthTokenUrl(config.getOauthTokenUrl())
                .oauthUserInfoUrl(config.getOauthUserInfoUrl())
                .oauthRedirectUri(config.getOauthRedirectUri())
                .oauthScopes(config.getOauthScopes())
                .build();
    }
}
