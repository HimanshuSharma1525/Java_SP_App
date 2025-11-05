// SSOConfigRepository.java
package com.yourcompany.multitenant.repository;

import com.yourcompany.multitenant.model.SSOConfig;
import com.yourcompany.multitenant.model.SSOProvider;
import com.yourcompany.multitenant.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SSOConfigRepository extends JpaRepository<SSOConfig, Long> {
    Optional<SSOConfig> findByTenantAndProvider(Tenant tenant, SSOProvider provider);
    List<SSOConfig> findByTenant(Tenant tenant);
    List<SSOConfig> findByTenantAndEnabledTrue(Tenant tenant);
}