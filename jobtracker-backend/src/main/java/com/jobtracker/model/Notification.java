package com.jobtracker.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "notifications")
public class Notification {

    @Id
    private String id;

    @Indexed
    private String userId;           // owner of the notification
    private String applicationId;    // optional link to application
    private String message;          // "Follow up on Google interview"

    private Instant eventDate;
    
    @Indexed
    private Instant notifyAt;  // when to trigger
    private boolean sent = false;    // mark after delivery

    private Instant createdAt;

    @Indexed
    private boolean read = false;    // mark after reading
    
    @Indexed
    private NotificationType type; // default type

    public enum NotificationType {
        FOLLOW_UP,
        INTERVIEW,
        DEADLINE,
        CUSTOM,
        STATUS_CHANGE
    }
}
