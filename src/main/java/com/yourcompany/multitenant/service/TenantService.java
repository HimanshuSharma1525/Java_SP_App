package com.yourcompany.multitenant.service;

import com.yourcompany.multitenant.config.TenantContext;
import com.yourcompany.multitenant.exception.TenantNotFoundException;
import com.yourcompany.multitenant.model.Tenant;
import com.yourcompany.multitenant.repository.TenantRepository;
import com.yourcompany.multitenant.config.TenantFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;

    @Transactional(readOnly = true)
    public Tenant getCurrentTenant() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new TenantNotFoundException("No tenant context found");
        }

        // 🎯 1. Handle Super Admin Login (base domain: localhost or yourdomain.com)
        // If the context ID is the special identifier from TenantFilter,
        // we must look up the tenant by its actual database subdomain, which is the empty string ('').
        if (TenantFilter.SUPER_ADMIN_ID.equals(tenantId)) {
            log.debug("Current context is Super Admin. Fetching tenant by empty subdomain ('').");

            return tenantRepository.findBySubdomain("")
                    .orElseThrow(() -> new TenantNotFoundException("Super Admin tenant not found (no subdomain match)."));
        }

        // 2. Handle Regular Tenant Login (subdomain: tenant1.localhost or tenant1.yourdomain.com)
        log.debug("Current context is regular tenant: {}. Fetching tenant by subdomain.", tenantId);
        return tenantRepository.findBySubdomain(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + tenantId));
    }

    @Transactional(readOnly = true)
    public Tenant getTenantBySubdomain(String subdomain) {
        return tenantRepository.findBySubdomain(subdomain)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + subdomain));
    }

    @Transactional(readOnly = true)
    public Optional<Tenant> getTenantBySubdomainOptional(String subdomain) {
        return tenantRepository.findBySubdomain(subdomain);
    }

    @Transactional
    public Tenant createTenant(String subdomain, String name) {
        if (tenantRepository.findBySubdomain(subdomain).isPresent()) {
            throw new IllegalArgumentException("Tenant with subdomain already exists: " + subdomain);
        }

        Tenant tenant = Tenant.builder()
                .subdomain(subdomain)
                .name(name)
                .active(true)
                .build();

        return tenantRepository.save(tenant);
    }

    // ✅ New method: create tenant in a new transaction
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Tenant createTenantInNewTransaction(String subdomain, String name) {
        log.info("Creating tenant in new transaction: {}", subdomain);
        return createTenant(subdomain, name);
    }

    @Transactional
    public Tenant updateTenant(Long id, String name) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + id));

        tenant.setName(name);
        return tenantRepository.save(tenant);
    }

    @Transactional
    public void deleteTenant(Long id) {
        if (!tenantRepository.existsById(id)) {
            throw new TenantNotFoundException("Tenant not found: " + id);
        }
        tenantRepository.deleteById(id);
    }

    public boolean isSuperAdminTenant() {
        String tenantId = TenantContext.getTenantId();
        // Check if the context ID is the special Super Admin identifier
        return TenantFilter.SUPER_ADMIN_ID.equals(tenantId);
    }
}