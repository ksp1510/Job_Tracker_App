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

    @NotBlank(message = "Password is required")
    private String passwordHash;

    @NotBlank(message = "Role is required")
    private String role; // admin | user
    private final Instant createdAt = Instant.now();

    private boolean notificationEnabled = true;
    private boolean emailNotificationsEnabled = true;
    private boolean inAppNotificationsEnabled = true;
}
