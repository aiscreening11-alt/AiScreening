package com.company.recruitment.controller;

import com.company.recruitment.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(ApiResponse.success(null, "Not authenticated"));
        }

        Map<String, Object> userData = Map.of(
                "username", authentication.getName(),
                "role", "HR Administrator",
                "isAdmin", true,
                "authorities", authentication.getAuthorities()
        );

        return ResponseEntity.ok(ApiResponse.success(userData, "HR Admin authenticated successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(Authentication authentication) {
        return getCurrentUser(authentication);
    }
}
