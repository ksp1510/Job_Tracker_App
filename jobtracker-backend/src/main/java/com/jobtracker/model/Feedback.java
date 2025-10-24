package com.jobtracker.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "feedback")
public class Feedback {
    @Id
    private String id;

    private String userId; // Optional - can be null for anonymous feedback

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Feedback type is required")
    private String type; // BUG, FEATURE_REQUEST, GENERAL, IMPROVEMENT

    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private Integer rating; // 1-5 stars

    @NotBlank(message = "Message is required")
    private String message;

    private String status; // NEW, IN_REVIEW, RESOLVED, CLOSED

    private String response; // Admin response

    private Instant createdAt = Instant.now();

    private Instant updatedAt;
}
