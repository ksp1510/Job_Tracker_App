package com.jobtracker.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.Data;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import com.jobtracker.model.User;
import com.jobtracker.repository.UserRepository;
import com.jobtracker.config.JwtUtil;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    // FIXED: Added notification preference fields to response
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
}