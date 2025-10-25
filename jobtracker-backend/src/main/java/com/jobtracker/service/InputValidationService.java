package com.jobtracker.service;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * SECURITY: Input validation service to prevent NoSQL injection and other attacks
 */
@Service
public class InputValidationService {

    // Pattern to detect MongoDB operators
    private static final Pattern MONGODB_OPERATOR_PATTERN = Pattern.compile(".*\\$.*");

    // Pattern to detect JavaScript code injection attempts
    private static final Pattern JAVASCRIPT_PATTERN = Pattern.compile(".*<script.*|.*javascript:.*", Pattern.CASE_INSENSITIVE);

    // Pattern for valid email (additional layer beyond @Email annotation)
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // Pattern for alphanumeric with common characters (names, titles)
    private static final Pattern SAFE_STRING_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s.,!?'\"\\-()&@#+]*$");

    /**
     * Sanitize string input to prevent NoSQL injection
     * Removes MongoDB operators like $where, $regex, $gt, etc.
     */
    public String sanitizeString(String input) {
        if (input == null) {
            return null;
        }

        // Remove MongoDB operators
        if (MONGODB_OPERATOR_PATTERN.matcher(input).matches()) {
            throw new IllegalArgumentException("Invalid input: MongoDB operators not allowed");
        }

        // Remove JavaScript injection attempts
        if (JAVASCRIPT_PATTERN.matcher(input).matches()) {
            throw new IllegalArgumentException("Invalid input: Script injection detected");
        }

        return input.trim();
    }

    /**
     * Validate email format
     */
    public boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Validate safe string (for names, titles, etc.)
     */
    public boolean isSafeString(String input) {
        if (input == null || input.isEmpty()) {
            return true; // Empty is safe
        }
        return SAFE_STRING_PATTERN.matcher(input).matches();
    }

    /**
     * Validate search query to prevent injection
     */
    public String sanitizeSearchQuery(String query) {
        if (query == null) {
            return null;
        }

        String sanitized = sanitizeString(query);

        // Additional check for search queries
        if (sanitized.length() > 200) {
            throw new IllegalArgumentException("Search query too long (max 200 characters)");
        }

        return sanitized;
    }

    /**
     * Validate and sanitize job title/company name
     */
    public String sanitizeJobField(String field) {
        if (field == null) {
            return null;
        }

        String sanitized = sanitizeString(field);

        if (sanitized.length() > 100) {
            throw new IllegalArgumentException("Field too long (max 100 characters)");
        }

        return sanitized;
    }

    /**
     * Validate numeric input
     */
    public Integer validateInteger(Integer value, int min, int max) {
        if (value == null) {
            return null;
        }

        if (value < min || value > max) {
            throw new IllegalArgumentException(
                String.format("Value must be between %d and %d", min, max)
            );
        }

        return value;
    }

    /**
     * Validate status enum values
     */
    public String validateStatus(String status, String[] allowedValues) {
        if (status == null) {
            return null;
        }

        for (String allowed : allowedValues) {
            if (allowed.equalsIgnoreCase(status)) {
                return allowed.toUpperCase();
            }
        }

        throw new IllegalArgumentException("Invalid status value: " + status);
    }
}
