// UserRepository.java
package com.yourcompany.multitenant.repository;

import com.yourcompany.multitenant.model.Role;
import com.yourcompany.multitenant.model.Tenant;
import com.yourcompany.multitenant.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findByTenantAndRole(Tenant tenant, Role role);
    List<User> findByTenant(Tenant tenant);
    boolean existsByEmail(String email);
}