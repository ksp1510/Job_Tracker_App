package com.jobtracker.controller;

import org.springframework.web.bind.annotation.*;

import com.jobtracker.model.User;
import com.jobtracker.repository.UserRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;


import java.util.Optional;


@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {
    
    @Autowired
    private UserRepository userRepository;
    
    
    private PasswordEncoder passwordEncoder;
    

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        try {
            // Get the authenticated user's ID from the JWT token
            String userId = getUserIdFromToken();

            if (userId == null) {
                return ResponseEntity.status(401).body("Unauthorized");
            }

            // Find user in MongoDB
            Optional<User> userOptional = userRepository.findById(userId);

            if (userOptional.isEmpty()) {
                return ResponseEntity.status(404).body("User not found");
            }

            User user = userOptional.get();

            // Return the user data
            return ResponseEntity.ok(user);

        } catch (Exception e) {
            log.error("Error getting current user", e);
            return ResponseEntity.status(500).body("Internal Server Error");
        }
    }

    private String getUserIdFromToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            return jwt.getSubject();
        }
        return null;
    }

    public static String  getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            return jwt.getSubject();
        }
        throw new RuntimeException("No authenticated user found");
    }

    public static String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            return jwt.getClaim("email");
        }
        return null;
    }

    @PostMapping("/change-password")
    public ResponseEntity<MessageResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        
        String userId = getCurrentUserId();
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            return ResponseEntity.badRequest()
                .body(new MessageResponse("Current password is incorrect"));
        }
        
        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        return ResponseEntity.ok(new MessageResponse("Password changed successfully"));
    }

    @PostMapping("/change-email")
    public ResponseEntity<MessageResponse> changeEmail(
            @Valid @RequestBody ChangeEmailRequest request) {
        
        String userId = getCurrentUserId();
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            return ResponseEntity.badRequest()
                .body(new MessageResponse("Password is incorrect"));
        }
        
        // Check if email already exists
        if (userRepository.findByEmail(request.getNewEmail()).isPresent()) {
            return ResponseEntity.badRequest()
                .body(new MessageResponse("Email already in use"));
        }
        
        // Update email
        user.setEmail(request.getNewEmail());
        userRepository.save(user);
        
        return ResponseEntity.ok(new MessageResponse("Email changed successfully"));
    }

    @DeleteMapping("/delete-account")
    public ResponseEntity<MessageResponse> deleteAccount() {
        
        String userId = getCurrentUserId();
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        // Delete user and all associated data
        // Note: You should also delete related applications, notifications, etc.
        // Consider using @PreRemove or cascade delete in your entities
        userRepository.delete(user);
        
        return ResponseEntity.ok(new MessageResponse("Account deleted successfully"));
    }

    @Data
    @AllArgsConstructor
    static class UserProfileResponse {
        private String userId;
        private String firstName;
        private String lastName;
        private String email;
    }

    @Data
    static class NotificationPreferencesRequest {
        private boolean notificationsEnabled = true;
        private boolean emailEnabled = true;
        private boolean inAppEnabled = true;
    }

    @Data
    static class ChangePasswordRequest {
        @NotBlank(message = "Current password is required")
        private String currentPassword;
        
        @NotBlank(message = "New password is required")
        private String newPassword;
    }

    @Data
    static class ChangeEmailRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String newEmail;
        
        @NotBlank(message = "Password is required")
        private String password;
    }

    @Data
    @AllArgsConstructor
    static class MessageResponse {
        private String message;
    }
}