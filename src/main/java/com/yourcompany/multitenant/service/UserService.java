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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TenantService tenantService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserDTO> getAllCustomerAdmins() {
        List<User> customerAdmins = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.CUSTOMER_ADMIN)
                .collect(Collectors.toList());

        return customerAdmins.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getEndUsersByTenant() {
        Tenant tenant = tenantService.getCurrentTenant();
        List<User> endUsers = userRepository.findByTenantAndRole(tenant, Role.END_USER);

        return endUsers.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Fetch a single user by ID, validating access and role.
     * Used by CustomerAdminController for edit modal pre-filling.
     */
    @Transactional(readOnly = true)
    public UserDTO getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        // Ensure the current administrator can access this user
        validateUserAccess(user);

        // Ensure the user has the role expected by the controller context
        if (user.getRole() != Role.END_USER) {
            throw new UnauthorizedAccessException("Access denied. User role is not END_USER.");
        }

        return convertToDTO(user);
    }

    @Transactional
    public UserDTO createCustomerAdmin(CreateUserRequest request) {
        if (request.getTenantSubdomain() == null || request.getTenantSubdomain().isEmpty()) {
            throw new IllegalArgumentException("Tenant subdomain is required for customer admin");
        }

        // 1️⃣ Check if tenant exists
        Optional<Tenant> tenantOpt = tenantService.getTenantBySubdomainOptional(request.getTenantSubdomain());

        // 2️⃣ Create tenant if not exists
        Tenant tenant = tenantOpt.orElseGet(() ->
                tenantService.createTenantInNewTransaction(
                        request.getTenantSubdomain(),
                        request.getTenantSubdomain() + " Organization"
                )
        );

        // 3️⃣ Check if email exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // 4️⃣ Build and save user
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
        Tenant tenant = tenantService.getCurrentTenant();

        // Email must be unique within the current tenant
        if (userRepository.existsByEmailAndTenant(request.getEmail(), tenant)) {
            throw new IllegalArgumentException("Email already exists in this tenant");
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

        validateUserAccess(user);

        // CustomerAdmin should only be allowed to update END_USERs via this path
        if (user.getRole() != Role.END_USER) {
            throw new UnauthorizedAccessException("Cannot modify user with role: " + user.getRole().name());
        }

        // 1. Update Name Fields
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());

        // 2. Update Email (Requires uniqueness check if changed)
        if (request.getEmail() != null && !request.getEmail().isEmpty() && !user.getEmail().equalsIgnoreCase(request.getEmail())) {
            // Assume UserRepository has 'existsByEmailAndTenant' matching the unique constraint
            if (userRepository.existsByEmailAndTenant(request.getEmail(), user.getTenant())) {
                throw new IllegalArgumentException("Email " + request.getEmail() + " already exists for another user in this tenant.");
            }
            user.setEmail(request.getEmail());
        }

        // 3. Update Password (Only if provided)
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        // 4. Update Active Status
        if (request.getActive() != null) user.setActive(request.getActive());

        user = userRepository.save(user);
        return convertToDTO(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        validateUserAccess(user);

        // Safety check: only allow deleting END_USERs
        if (user.getRole() != Role.END_USER) {
            throw new UnauthorizedAccessException("Cannot delete user with role: " + user.getRole().name());
        }

        userRepository.delete(user);
    }

    private void validateUserAccess(User user) {
        Tenant currentTenant = tenantService.getCurrentTenant();

        // Super Admin can access any user
        if (tenantService.isSuperAdminTenant()) return;

        // Customer Admin can only access users within their own tenant
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