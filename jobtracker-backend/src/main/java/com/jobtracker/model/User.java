package com.jobtracker.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "users")
public class User {
    @Id
    private String userId;
    private String name;
    private String email;
    private String passwordHash;
    private String role; // admin | user
    private Instant createdAt = Instant.now();
}
