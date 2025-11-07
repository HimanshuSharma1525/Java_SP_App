package com.yourcompany.multitenant.service;

import com.yourcompany.multitenant.config.TenantContext;
import com.yourcompany.multitenant.exception.TenantNotFoundException;
import com.yourcompany.multitenant.model.Tenant;
import com.yourcompany.multitenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;

    @Transactional(readOnly = true)
    public Tenant getCurrentTenant() {
        String subdomain = TenantContext.getTenantId();
        if (subdomain == null) {
            throw new TenantNotFoundException("No tenant context found");
        }

        return tenantRepository.findBySubdomain(subdomain)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + subdomain));
    }

    @Transactional(readOnly = true)
    public Tenant getTenantBySubdomain(String subdomain) {
        return tenantRepository.findBySubdomain(subdomain)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + subdomain));
    }

    @Transactional(readOnly = true)
    public java.util.Optional<Tenant> getTenantBySubdomainOptional(String subdomain) {
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
        String subdomain = TenantContext.getTenantId();
        return "superadmin".equals(subdomain);
    }
}
