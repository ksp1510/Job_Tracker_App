package com.jobtracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.exception.AuthenticationException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Service to interact with Auth0 Authentication and Management APIs
 */
@Slf4j
@Service
public class Auth0Service {

    @Value("${auth0.domain}")
    private String auth0Domain;

    @Value("${auth0.clientId}")
    private String clientId;

    @Value("${auth0.clientSecret}")
    private String clientSecret;

    @Value("${auth0.audience}")
    private String audience;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Authenticate user with email/password using Auth0's Resource Owner Password Grant
     * This allows custom login UI while using Auth0 for authentication
     */
    public Auth0LoginResponse authenticate(String email, String password) {
        try {
            String url = String.format("https://%s/oauth/token", auth0Domain);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("grant_type", "password");
            body.put("username", email);
            body.put("password", password);
            body.put("client_id", clientId);
            body.put("client_secret", clientSecret);
            body.put("audience", audience);
            body.put("scope", "openid profile email");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                
                Auth0LoginResponse loginResponse = new Auth0LoginResponse();
                loginResponse.setAccessToken(jsonNode.get("access_token").asText());
                loginResponse.setIdToken(jsonNode.get("id_token").asText());
                loginResponse.setTokenType(jsonNode.get("token_type").asText());
                loginResponse.setExpiresIn(jsonNode.get("expires_in").asInt());

                // Get user info from ID token
                Auth0UserInfo userInfo = getUserInfoFromIdToken(loginResponse.getIdToken());
                loginResponse.setUserInfo(userInfo);

                log.info("User authenticated successfully: {}", email);
                return loginResponse;
            }

            throw new AuthenticationException("Authentication failed");

        } catch (HttpClientErrorException e) {
            log.error("Auth0 authentication error: {}", e.getResponseBodyAsString());
            if (e.getStatusCode() == HttpStatus.FORBIDDEN || e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new AuthenticationException("Invalid email or password");
            }
            throw new AuthenticationException("Authentication failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error during Auth0 authentication", e);
            throw new AuthenticationException("Authentication failed: " + e.getMessage());
        }
    }

    /**
     * Create a new user in Auth0
     */
    public Auth0User createUser(String firstName, String lastName, String email, String password) {
        try {
            // First, get management API token
            String managementToken = getManagementApiToken();

            String url = String.format("https://%s/api/v2/users", auth0Domain);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(managementToken);

            Map<String, Object> body = new HashMap<>();
            body.put("email", email);
            body.put("password", password);
            body.put("connection", "Username-Password-Authentication");
            body.put("email_verified", false);
            
            // Add user metadata
            Map<String, Object> userMetadata = new HashMap<>();
            userMetadata.put("first_name", firstName);
            userMetadata.put("last_name", lastName);
            body.put("user_metadata", userMetadata);

            // Set name for display
            body.put("name", firstName + " " + lastName);
            body.put("given_name", firstName);
            body.put("family_name", lastName);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode() == HttpStatus.CREATED && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                
                Auth0User user = new Auth0User();
                user.setUserId(jsonNode.get("user_id").asText());
                user.setEmail(jsonNode.get("email").asText());
                user.setEmailVerified(jsonNode.get("email_verified").asBoolean());
                user.setName(jsonNode.get("name").asText());
                
                if (jsonNode.has("given_name")) {
                    user.setGivenName(jsonNode.get("given_name").asText());
                }
                if (jsonNode.has("family_name")) {
                    user.setFamilyName(jsonNode.get("family_name").asText());
                }

                log.info("User created successfully in Auth0: {}", email);
                return user;
            }

            throw new RuntimeException("Failed to create user in Auth0");

        } catch (HttpClientErrorException e) {
            log.error("Auth0 user creation error: {}", e.getResponseBodyAsString());
            
            // Check for specific errors
            String errorBody = e.getResponseBodyAsString();
            if (errorBody.contains("user already exists") || errorBody.contains("The user already exists")) {
                throw new AuthenticationException("Email already registered");
            }
            if (errorBody.contains("PasswordStrengthError")) {
                throw new AuthenticationException("Password does not meet security requirements");
            }
            
            throw new AuthenticationException("Failed to create user: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error creating user in Auth0", e);
            throw new RuntimeException("Failed to create user: " + e.getMessage());
        }
    }

    /**
     * Get user info from Auth0 using access token
     */
    public Auth0UserInfo getUserInfo(String accessToken) {
        try {
            String url = String.format("https://%s/userinfo", auth0Domain);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                request, 
                String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                return parseUserInfo(jsonNode);
            }

            throw new RuntimeException("Failed to get user info");

        } catch (Exception e) {
            log.error("Error getting user info from Auth0", e);
            throw new RuntimeException("Failed to get user info: " + e.getMessage());
        }
    }

    /**
     * Get Management API token (for user creation and management)
     */
    private String getManagementApiToken() {
        try {
            String url = String.format("https://%s/oauth/token", auth0Domain);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("grant_type", "client_credentials");
            body.put("client_id", clientId);
            body.put("client_secret", clientSecret);
            body.put("audience", String.format("https://%s/api/v2/", auth0Domain));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                return jsonNode.get("access_token").asText();
            }

            throw new RuntimeException("Failed to get management API token");

        } catch (Exception e) {
            log.error("Error getting Auth0 management token", e);
            throw new RuntimeException("Failed to get management token: " + e.getMessage());
        }
    }

    /**
     * Parse user info from ID token (JWT)
     */
    private Auth0UserInfo getUserInfoFromIdToken(String idToken) {
        try {
            // Decode JWT payload (without verification - backend will verify)
            String[] parts = idToken.split("\\.");
            if (parts.length < 2) {
                throw new RuntimeException("Invalid ID token");
            }

            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            JsonNode jsonNode = objectMapper.readTree(payload);

            return parseUserInfo(jsonNode);

        } catch (Exception e) {
            log.error("Error parsing ID token", e);
            throw new RuntimeException("Failed to parse ID token: " + e.getMessage());
        }
    }

    /**
     * Parse user info from JSON node
     */
    private Auth0UserInfo parseUserInfo(JsonNode jsonNode) {
        Auth0UserInfo userInfo = new Auth0UserInfo();
        
        userInfo.setSub(jsonNode.has("sub") ? jsonNode.get("sub").asText() : null);
        userInfo.setEmail(jsonNode.has("email") ? jsonNode.get("email").asText() : null);
        userInfo.setEmailVerified(jsonNode.has("email_verified") ? jsonNode.get("email_verified").asBoolean() : false);
        userInfo.setName(jsonNode.has("name") ? jsonNode.get("name").asText() : null);
        userInfo.setGivenName(jsonNode.has("given_name") ? jsonNode.get("given_name").asText() : null);
        userInfo.setFamilyName(jsonNode.has("family_name") ? jsonNode.get("family_name").asText() : null);
        userInfo.setNickname(jsonNode.has("nickname") ? jsonNode.get("nickname").asText() : null);
        userInfo.setPicture(jsonNode.has("picture") ? jsonNode.get("picture").asText() : null);

        return userInfo;
    }

    /**
     * Change user password in Auth0
     */
    public void changePassword(String userId, String newPassword) {
        try {
            String managementToken = getManagementApiToken();
            String url = String.format("https://%s/api/v2/users/%s", auth0Domain, userId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(managementToken);

            Map<String, Object> body = new HashMap<>();
            body.put("password", newPassword);
            body.put("connection", "Username-Password-Authentication");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            restTemplate.exchange(url, HttpMethod.PATCH, request, String.class);

            log.info("Password changed successfully for user: {}", userId);

        } catch (Exception e) {
            log.error("Error changing password in Auth0", e);
            throw new RuntimeException("Failed to change password: " + e.getMessage());
        }
    }

    /**
     * Send password reset email
     */
    public void sendPasswordResetEmail(String email) {
        try {
            String url = String.format("https://%s/dbconnections/change_password", auth0Domain);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("client_id", clientId);
            body.put("email", email);
            body.put("connection", "Username-Password-Authentication");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            restTemplate.postForEntity(url, request, String.class);

            log.info("Password reset email sent to: {}", email);

        } catch (Exception e) {
            log.error("Error sending password reset email", e);
            throw new RuntimeException("Failed to send password reset email: " + e.getMessage());
        }
    }

    // DTOs

    @Data
    public static class Auth0LoginResponse {
        private String accessToken;
        private String idToken;
        private String tokenType;
        private int expiresIn;
        private Auth0UserInfo userInfo;
    }

    @Data
    public static class Auth0User {
        private String userId;
        private String email;
        private boolean emailVerified;
        private String name;
        private String givenName;
        private String familyName;
    }

    @Data
    public static class Auth0UserInfo {
        private String sub;
        private String email;
        private boolean emailVerified;
        private String name;
        private String givenName;
        private String familyName;
        private String nickname;
        private String picture;
    }
}