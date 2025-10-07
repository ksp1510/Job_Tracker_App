package com.jobtracker.controller;

import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import lombok.Data;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.jobtracker.model.User;
import com.jobtracker.repository.UserRepository;
import com.jobtracker.config.JwtUtil;
import com.jobtracker.service.NotificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(
            @RequestHeader("Authorization") String authorization) {
        String token = authorization.replace("Bearer ", "");
        String userId = jwtUtil.getUserId(token);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return ResponseEntity.ok(
            new UserProfileResponse(
                user.getUserId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.isNotificationEnabled(),
                user.isEmailNotificationsEnabled(),
                user.isInAppNotificationsEnabled()
            )
        );
    }

    @PutMapping("/notification-preferences")
    public ResponseEntity<User> updateNotificationPreferences(
            @Valid @RequestBody NotificationPreferencesRequest request,
            @RequestHeader("Authorization") String authorization) {
        
        String token = authorization.replace("Bearer ", "");
        String userId = jwtUtil.getUserId(token);
        
        User updatedUser = notificationService.updateNotificationPreferences(
            userId,
            request.isNotificationsEnabled(),
            request.isEmailEnabled(),
            request.isInAppEnabled()
        );
        
        return ResponseEntity.ok(updatedUser);
    }

    @PostMapping("/change-password")
    public ResponseEntity<MessageResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @RequestHeader("Authorization") String authorization) {
        
        String token = authorization.replace("Bearer ", "");
        String userId = jwtUtil.getUserId(token);
        
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
            @Valid @RequestBody ChangeEmailRequest request,
            @RequestHeader("Authorization") String authorization) {
        
        String token = authorization.replace("Bearer ", "");
        String userId = jwtUtil.getUserId(token);
        
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
    public ResponseEntity<MessageResponse> deleteAccount(
            @RequestHeader("Authorization") String authorization) {
        
        String token = authorization.replace("Bearer ", "");
        String userId = jwtUtil.getUserId(token);
        
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
        private String role;
        private boolean notificationEnabled;
        private boolean emailNotificationsEnabled;
        private boolean inAppNotificationsEnabled;
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