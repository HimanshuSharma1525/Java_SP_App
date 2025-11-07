// src/main/java/com/yourcompany/multitenant/repository/SSOConfigRepository.java
package com.yourcompany.multitenant.repository;

import com.yourcompany.multitenant.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SSOConfigRepository extends JpaRepository<SSOConfig, Long> {
    Optional<SSOConfig> findByTenantAndProvider(Tenant tenant, SSOProvider provider);
    List<SSOConfig> findByTenant(Tenant tenant);
}
