// EndUserController.java
package com.yourcompany.multitenant.controller;

import com.yourcompany.multitenant.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/end-user")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'CUSTOMER_ADMIN', 'END_USER')")
public class EndUserController {

    @GetMapping("/profile")
    public ResponseEntity<UserDTO> getProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        // Return basic user info
        UserDTO userDTO = UserDTO.builder()
                .email(email)
                .build();

        return ResponseEntity.ok(userDTO);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<String> getDashboard() {
        return ResponseEntity.ok("Welcome to End User Dashboard");
    }
}