package com.jobtracker.controller;

import com.jobtracker.model.User;
import com.jobtracker.repository.UserRepository;
import com.jobtracker.config.RateLimitService;
import com.jobtracker.service.Auth0Service;
import com.jobtracker.service.Auth0Service.Auth0LoginResponse;
import com.jobtracker.service.Auth0Service.Auth0User;
import com.jobtracker.service.Auth0Service.Auth0UserInfo;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;

import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private Auth0Service auth0Service;

    @Autowired
    private RateLimitService rateLimitService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        // SECURITY: Rate limiting - 10 registrations per hour per IP
        String clientIp = getClientIP(httpRequest);
        Bucket bucket = rateLimitService.resolveBucketForRegistration(clientIp);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Too many registration attempts. Please try again in " + waitForRefill + " seconds");
        }

        try {

            // Check if email already exists in MongoDB
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ErrorResponse("This Email is already registered"));
            }

            // Validate password strength
            validatePassword(request.getPassword());

            // Create user in Auth0
            Auth0User auth0User = auth0Service.createUser(
                request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                request.getPassword()
            );

            // Create user in MongoDB with Auth0 user ID
            User user = new User();
            user.setUserId(auth0User.getUserId());
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setEmail(request.getEmail());
            user.setPasswordHash(null);
            user.setAuthProvider("AUTH0");

            User savedUser = userRepository.save(user);

            log.info("User registered successfully: {}", request.getEmail());

            return ResponseEntity.ok(new RegisterResponse(
                savedUser.getUserId(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getEmail(),
                "Registration successful. Please check your email for confirmation."
            ));
        } catch (com.jobtracker.exception.AuthenticationException e) {
            log.error("Auth0 registration error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Auth0 registration error: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Registration error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Registration failed. Please try again later."));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        // Rate limiting - 5 login attempts per minute per ip
        String clientIp = getClientIP(httpRequest);
        Bucket bucket = rateLimitService.resolveBucketForLogin(clientIp);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Too many login attempts. Please try again in " + waitForRefill + " seconds");
        }

        try {
            Auth0LoginResponse auth0LoginResponse = auth0Service.authenticate(
                request.getEmail(),
                request.getPassword()
            );

            Auth0UserInfo userInfo = auth0LoginResponse.getUserInfo();

            // Sync/Update user in MongoDB
            Optional<User> existingUser = userRepository.findByEmail(request.getEmail());
            User user;

            if (existingUser.isPresent()) {
                user = existingUser.get();
                // Update user ID if it changed
                if (!user.getUserId().equals(userInfo.getSub())) {
                    user.setUserId(userInfo.getSub());
                    user = userRepository.save(user);
                }
            } else {
                // User logged in via Auth0 but not in MongoDB
                // Create user in MongoDB
                user = new User();
                user.setUserId(userInfo.getSub());
                user.setFirstName(userInfo.getGivenName() != null ? userInfo.getGivenName() : "");
                user.setLastName(userInfo.getFamilyName() != null ? userInfo.getFamilyName() : "");
                user.setEmail(userInfo.getEmail());
                user.setPasswordHash(null);
                user.setAuthProvider("AUTH0");
                user = userRepository.save(user);
            }

            log.info("User logged in successfully: {}", request.getEmail());

            // Return Auth0 access token
            return ResponseEntity.ok(new LoginResponse(
                auth0LoginResponse.getAccessToken(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail()
            ));
        
        } catch (com.jobtracker.exception.AuthenticationException e) {
            log.error("Auth0 login error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid email or password"));
        } catch (Exception e) {
            log.error("Auth0 login error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Login failed. Please try again later."));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            log.info("Password reset request received for email: {}", request.getEmail());
            
            auth0Service.sendPasswordResetEmail(request.getEmail());
            
            log.info("✅ Password reset email sent successfully to: {}", request.getEmail());
            
            return ResponseEntity.ok(new MessageResponse(
                "Password reset email sent successfully"
            ));
            
        } catch (Exception e) {
            log.error("❌ Password reset email error for: {}", request.getEmail(), e);
            
            // Return generic success message for security
            // (don't reveal if email exists or not)
            return ResponseEntity.ok(new MessageResponse(
                "If an account exists with this email, a password reset email has been sent."
            ));
        }
    }

    /**
     * Get client IP address, considering proxy headers
     */
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
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
        private final String email;
    }

    @Data
    static class ForgotPasswordRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;
    }

    @Data
    static class RegisterResponse {
        private final String userId;
        private final String firstName;
        private final String lastName;
        private final String email;
        private final String message;
    }

    @Data
    static class ErrorResponse {
        private final String message;
    }

    @Data
    static class MessageResponse {
        private final String message;
    }
}
