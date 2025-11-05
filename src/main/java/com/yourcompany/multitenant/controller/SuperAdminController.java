// SuperAdminController.java
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
@RequestMapping("/api/super-admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class SuperAdminController {

    private final UserService userService;

    @GetMapping("/customer-admins")
    public ResponseEntity<List<UserDTO>> getAllCustomerAdmins() {
        List<UserDTO> customerAdmins = userService.getAllCustomerAdmins();
        return ResponseEntity.ok(customerAdmins);
    }

    @PostMapping("/customer-admins")
    public ResponseEntity<UserDTO> createCustomerAdmin(@Valid @RequestBody CreateUserRequest request) {
        UserDTO user = userService.createCustomerAdmin(request);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/users/{userId}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long userId,
                                              @Valid @RequestBody UpdateUserRequest request) {
        UserDTO user = userService.updateUser(userId, request);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}