package com.yourcompany.multitenant.controller;

import com.yourcompany.multitenant.model.SSOConfig;
import com.yourcompany.multitenant.repository.SSOConfigRepository;
import com.yourcompany.multitenant.model.Tenant;
import com.yourcompany.multitenant.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sso")
@RequiredArgsConstructor
public class PublicSSOController {

    private final SSOConfigRepository ssoConfigRepository;
    private final TenantService tenantService;

    // Public endpoint: returns enabled SSO providers for current tenant
    @GetMapping("/providers")
    public ResponseEntity<List<String>> getEnabledSSOProviders() {
        Tenant tenant = tenantService.getCurrentTenant();

        List<String> providers = ssoConfigRepository.findByTenantAndEnabledTrue(tenant).stream()
                .map(config -> config.getProvider().name())
                .toList();

        return ResponseEntity.ok(providers);
    }
}
