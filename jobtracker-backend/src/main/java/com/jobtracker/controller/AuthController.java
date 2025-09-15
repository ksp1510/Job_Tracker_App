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
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setRole(request.role);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword())); // hash password
        return userRepository.save(user);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        Optional<User> user = userRepository.findByEmail(request.getEmail());
        if (user.isPresent() && passwordEncoder.matches(request.getPassword(), user.get().getPasswordHash())) {
            String token = jwtUtil.generateToken(user.get().getUserId(), user.get().getRole());
            return new LoginResponse(token, user.get().getRole());
        }
        throw new RuntimeException("Invalid credentials");
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
        private String role = "USER"; // e.g., Temp set to USER for easy testing
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
        private final String role;
    }
}
