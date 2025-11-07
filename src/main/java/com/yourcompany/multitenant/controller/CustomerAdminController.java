package com.yourcompany.multitenant.controller;

import com.yourcompany.multitenant.dto.CreateUserRequest;
import com.yourcompany.multitenant.dto.UpdateUserRequest;
import com.yourcompany.multitenant.dto.UserDTO;
import com.yourcompany.multitenant.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer-admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'CUSTOMER_ADMIN')")
public class CustomerAdminController {

    private final UserService userService;

    // ✅ Fetch all END_USERS under current tenant
    @GetMapping("/end-users")
    public ResponseEntity<List<UserDTO>> getEndUsers() {
        List<UserDTO> endUsers = userService.getEndUsersByTenant();
        return ResponseEntity.ok(endUsers);
    }

    // ✅ Create END_USER under current tenant
    @PostMapping("/end-users")
    public ResponseEntity<UserDTO> createEndUser(@Valid @RequestBody CreateUserRequest request) {
        UserDTO createdUser = userService.createEndUser(request);
        return ResponseEntity.ok(createdUser);
    }

    // ✅ Update END_USER under current tenant
    @PutMapping("/end-users/{userId}")
    public ResponseEntity<UserDTO> updateEndUser(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest request) {
        UserDTO updatedUser = userService.updateUser(userId, request);
        return ResponseEntity.ok(updatedUser);
    }

    // ✅ Delete END_USER under current tenant
    @DeleteMapping("/end-users/{userId}")
    public ResponseEntity<Void> deleteEndUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
