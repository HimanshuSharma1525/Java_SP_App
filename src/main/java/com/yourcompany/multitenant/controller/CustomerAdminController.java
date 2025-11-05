// CustomerAdminController.java
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

    @GetMapping("/end-users")
    public ResponseEntity<List<UserDTO>> getEndUsers() {
        List<UserDTO> endUsers = userService.getEndUsersByTenant();
        return ResponseEntity.ok(endUsers);
    }

    @PostMapping("/end-users")
    public ResponseEntity<UserDTO> createEndUser(@Valid @RequestBody CreateUserRequest request) {
        UserDTO user = userService.createEndUser(request);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/end-users/{userId}")
    public ResponseEntity<UserDTO> updateEndUser(@PathVariable Long userId,
                                                 @Valid @RequestBody UpdateUserRequest request) {
        UserDTO user = userService.updateUser(userId, request);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/end-users/{userId}")
    public ResponseEntity<Void> deleteEndUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}