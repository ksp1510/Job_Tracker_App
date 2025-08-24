package com.jobtracker.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "notifications")
public class Notification {

    @Id
    private String id;

    private String userId;           // owner of the notification
    private String applicationId;    // optional link to application
    private String message;          // "Follow up on Google interview"
    private LocalDateTime notifyAt;  // when to trigger
    private boolean sent = false;    // mark after delivery
    private boolean read = false;    // mark after reading
    private Channel channel = Channel.IN_APP; // default channel
    private NotificationType type = NotificationType.FOLLOW_UP; // default type

    public enum Channel {
        IN_APP,
        EMAIL
    }

    public enum NotificationType {
        FOLLOW_UP,
        INTERVIEW,
        DEADLINE
    }
}
