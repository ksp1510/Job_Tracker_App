package com.jobtracker.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "notification_preferences")
public class NotificationPreference {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    private boolean emailEnabled = true;   // user-controlled
    private boolean inAppEnabled = true;   // enforced by backend
    private Instant updatedAt = Instant.now();

    // ✅ No-args constructor (required by Spring Data)
    public NotificationPreference() {
        this.emailEnabled = true;
        this.inAppEnabled = true;
        this.updatedAt = Instant.now();
    }

    // ✅ All-args constructor (for custom initialization)
    public NotificationPreference(String userId, boolean emailEnabled, boolean inAppEnabled, Instant updatedAt) {
        this.userId = userId;
        this.emailEnabled = emailEnabled;
        this.inAppEnabled = inAppEnabled;
        this.updatedAt = updatedAt;
    }

    // ✅ Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    public void setEmailEnabled(boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

    public boolean isInAppEnabled() {
        return inAppEnabled;
    }

    public void setInAppEnabled(boolean inAppEnabled) {
        this.inAppEnabled = inAppEnabled;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
