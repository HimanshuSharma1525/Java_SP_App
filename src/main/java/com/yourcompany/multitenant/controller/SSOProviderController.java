// SSOProviderController.java
package com.yourcompany.multitenant.controller;

import com.yourcompany.multitenant.model.SSOConfig;
import com.yourcompany.multitenant.model.SSOProvider;
import com.yourcompany.multitenant.model.Tenant;
import com.yourcompany.multitenant.repository.SSOConfigRepository;
import com.yourcompany.multitenant.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/sso")
@RequiredArgsConstructor
public class SSOProviderController {

    private final TenantService tenantService;
    private final SSOConfigRepository ssoConfigRepository;

    @GetMapping("/providers")
    public ResponseEntity<?> getProviders() {
        try {
            final Tenant tenant = tenantService.getCurrentTenant();
            if (tenant == null) {
                log.warn("❌ No tenant found in current context");
                return ResponseEntity.badRequest().body(Map.of("error", "Tenant not identified"));
            }

            List<SSOConfig> configs = ssoConfigRepository.findByTenant(tenant);
            Map<SSOProvider, Boolean> enabledMap = new EnumMap<>(SSOProvider.class);

            // Initialize all providers as disabled by default
            for (SSOProvider provider : SSOProvider.values()) {
                enabledMap.put(provider, false);
            }

            // Update with actual enabled status from DB
            if (configs != null && !configs.isEmpty()) {
                for (SSOConfig config : configs) {
                    enabledMap.put(config.getProvider(), Boolean.TRUE.equals(config.getEnabled()));
                }
            }

            // Prepare response list
            List<Map<String, Object>> providers = new ArrayList<>();
            enabledMap.forEach((provider, enabled) -> {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("provider", provider.name());
                entry.put("enabled", enabled);
                entry.put("loginUrl", switch (provider) {
                    case SAML -> "/sso/saml/login";
                    case JWT -> "/sso/jwt/login";
                    case OAUTH -> "/sso/oauth/login";
                });
                providers.add(entry);
            });

            log.info("✅ Returning SSO provider states for tenant '{}': {}", tenant.getSubdomain(), providers);
            return ResponseEntity.ok(providers);

        } catch (Exception e) {
            log.error("🔥 Error fetching SSO providers", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Internal Server Error",
                    "details", e.getMessage()
            ));
        }
    }
}
