// UserService.java
package com.yourcompany.multitenant.service;

import com.yourcompany.multitenant.dto.CreateUserRequest;
import com.yourcompany.multitenant.dto.UpdateUserRequest;
import com.yourcompany.multitenant.dto.UserDTO;
import com.yourcompany.multitenant.exception.UnauthorizedAccessException;
import com.yourcompany.multitenant.model.Role;
import com.yourcompany.multitenant.model.Tenant;
import com.yourcompany.multitenant.model.User;
import com.yourcompany.multitenant.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TenantService tenantService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserDTO> getAllCustomerAdmins() {
        // Only SUPER_ADMIN can call this
        List<User> customerAdmins = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.CUSTOMER_ADMIN)
                .collect(Collectors.toList());

        return customerAdmins.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getEndUsersByTenant() {
        // CUSTOMER_ADMIN calls this to get their end users
        Tenant tenant = tenantService.getCurrentTenant();
        List<User> endUsers = userRepository.findByTenantAndRole(tenant, Role.END_USER);

        return endUsers.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserDTO createCustomerAdmin(CreateUserRequest request) {
        // Only SUPER_ADMIN can create CUSTOMER_ADMIN
        if (request.getTenantSubdomain() == null) {
            throw new IllegalArgumentException("Tenant subdomain is required for customer admin");
        }

        // Create or get tenant
        Tenant tenant;
        try {
            tenant = tenantService.getTenantBySubdomain(request.getTenantSubdomain());
        } catch (Exception e) {
            // Create new tenant if doesn't exist
            tenant = tenantService.createTenant(
                    request.getTenantSubdomain(),
                    request.getTenantSubdomain() + " Organization"
            );
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(Role.CUSTOMER_ADMIN)
                .tenant(tenant)
                .active(true)
                .build();

        user = userRepository.save(user);
        return convertToDTO(user);
    }

    @Transactional
    public UserDTO createEndUser(CreateUserRequest request) {
        // CUSTOMER_ADMIN creates END_USER in their tenant
        Tenant tenant = tenantService.getCurrentTenant();

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(Role.END_USER)
                .tenant(tenant)
                .active(true)
                .build();

        user = userRepository.save(user);
        return convertToDTO(user);
    }

    @Transactional
    public UserDTO updateUser(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Validate access
        validateUserAccess(user);

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getActive() != null) {
            user.setActive(request.getActive());
        }

        user = userRepository.save(user);
        return convertToDTO(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Validate access
        validateUserAccess(user);

        userRepository.delete(user);
    }

    private void validateUserAccess(User user) {
        Tenant currentTenant = tenantService.getCurrentTenant();

        // Super admin can access anyone
        if (tenantService.isSuperAdminTenant()) {
            return;
        }

        // Customer admin can only access users in their tenant
        if (!user.getTenant().getId().equals(currentTenant.getId())) {
            throw new UnauthorizedAccessException("Cannot access users from different tenant");
        }
    }

    private UserDTO convertToDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .tenantId(user.getTenant().getId())
                .tenantSubdomain(user.getTenant().getSubdomain())
                .active(user.getActive())
                .build();
    }
}
