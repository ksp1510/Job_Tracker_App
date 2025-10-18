package com.jobtracker.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "notification_preferences")
public class NotificationPreference {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    private boolean emailEnabled = true;   // user-controlled
    private boolean inAppEnabled = true;   // enforced by backend
    private LocalDateTime updatedAt = LocalDateTime.now();

    // ✅ No-args constructor (required by Spring Data)
    public NotificationPreference() {
        this.emailEnabled = true;
        this.inAppEnabled = true;
        this.updatedAt = LocalDateTime.now();
    }

    // ✅ All-args constructor (for custom initialization)
    public NotificationPreference(String userId, boolean emailEnabled, boolean inAppEnabled, LocalDateTime updatedAt) {
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
