package com.jobtracker.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.persistence.Column;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "users")
@CompoundIndexes({
@CompoundIndex(name = "unique_email", def = "{'email': 1}", unique = true)
})
public class User {
    @Id
    private String userId;

    @NotBlank(message = "First Name is required")
    private String firstName;

    @NotBlank(message = "Last Name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    private String passwordHash; // Can be null for OAuth2 users

    private String authProvider; // "LOCAL", "GOOGLE", etc.

    private final Instant createdAt = Instant.now();

    @Column(name = "timezone")
    private String timezone = "America/Toronto"; // default

}
