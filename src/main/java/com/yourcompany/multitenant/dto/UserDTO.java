package com.yourcompany.multitenant.dto;

import com.yourcompany.multitenant.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for exposing safe User information to the frontend.
 *
 * Used in:
 * - Customer Admin End-User Management
 * - AuthController (Login Response)
 * - EndUserController (Profile)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private Long tenantId;
    private String tenantSubdomain;
    private Boolean active;

    /**
     * Converts a User entity into a safe UserDTO.
     */
    public static UserDTO fromEntity(User user) {
        if (user == null) return null;
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .tenantId(user.getTenant() != null ? user.getTenant().getId() : null)
                .tenantSubdomain(user.getTenant() != null ? user.getTenant().getSubdomain() : null)
                .active(user.getActive())
                .build();
    }
}
