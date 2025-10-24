package com.jobtracker.controller;

import com.jobtracker.model.User;
import com.jobtracker.repository.UserRepository;
import com.jobtracker.config.JwtUtil;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import java.util.Optional;

import com.jobtracker.exception.DuplicateEmailException;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public User register(@Valid @RequestBody RegisterRequest request) {
        // check if email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new DuplicateEmailException("Email already registered");
        }
        // SECURITY FIX: Validate password strength
        validatePassword(request.getPassword());

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword())); // hash password
        user.setAuthProvider("LOCAL");
        return userRepository.save(user);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        Optional<User> user = userRepository.findByEmail(request.getEmail());
        if (user.isPresent() && passwordEncoder.matches(request.getPassword(), user.get().getPasswordHash())) {
            String token = jwtUtil.generateToken(user.get().getUserId());
            // SECURITY FIX: Removed logging of tokens and PII
            return new LoginResponse(token, user.get().getFirstName(), user.get().getLastName());
        }
        throw new RuntimeException("Invalid credentials");
    }

    /**
     * SECURITY FIX: Validate password strength
     */
    private void validatePassword(String password) {
        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new IllegalArgumentException("Password must contain at least one lowercase letter");
        }
        if (!password.matches(".*\\d.*")) {
            throw new IllegalArgumentException("Password must contain at least one number");
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            throw new IllegalArgumentException("Password must contain at least one special character");
        }
    }

    // --- DTOs ---
    @Data
    static class RegisterRequest {
        @NotBlank(message = "First Name is required")
        private String firstName;
        @NotBlank(message = "Last Name is required")
        private String lastName;
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;
        @NotBlank(message = "Password is required")
        private String password;
    }

    @Data
    static class LoginRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;
        @NotBlank(message = "Password is required")
        private String password;
    }

    @Data
    static class LoginResponse {
        private final String token;
        private final String firstName;
        private final String lastName;
    }
}
