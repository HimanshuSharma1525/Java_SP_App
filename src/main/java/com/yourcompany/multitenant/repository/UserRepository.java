package com.yourcompany.multitenant.repository;

import com.yourcompany.multitenant.model.Role;
import com.yourcompany.multitenant.model.Tenant;
import com.yourcompany.multitenant.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailAndTenant(String email, Tenant tenant);
    List<User> findByTenant(Tenant tenant);

    boolean existsByEmail(@NotBlank(message = "Email is required") @Email(message = "Invalid email address") String email);

    /**
     * Checks if a user with the given email exists within the specified tenant.
     * This is crucial for enforcing tenant-scoped uniqueness for END_USERS.
     */
    boolean existsByEmailAndTenant(String email, Tenant tenant);

    List<User> findByTenantAndRole(Tenant tenant, Role role);
}