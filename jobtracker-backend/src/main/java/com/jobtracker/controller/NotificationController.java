package com.jobtracker.controller;

import com.jobtracker.config.JwtUtil;
import com.jobtracker.model.Application;
import com.jobtracker.model.Notification;
import com.jobtracker.model.User;
import com.jobtracker.repository.NotificationRepository;
import com.jobtracker.service.ApplicationService;
import com.jobtracker.service.NotificationService;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.time.Duration;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService service;
    private final NotificationRepository repository;
    private final ApplicationService applicationService;
    private final JwtUtil jwtUtil;

    public NotificationController(NotificationService service, 
                                NotificationRepository repository,
                                ApplicationService applicationService,
                                JwtUtil jwtUtil) {
        this.service = service;
        this.repository = repository;
        this.applicationService = applicationService;
        this.jwtUtil = jwtUtil;
    }

    // ================ USER NOTIFICATION MANAGEMENT ================

    /**
     * Get all notifications for logged-in user
     */
    @GetMapping
    public ResponseEntity<List<Notification>> getUserNotifications(
            @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        return ResponseEntity.ok(service.getUserNotifications(userId));
    }

    /**
     * Get unread notifications for logged-in user
     */
    @GetMapping("/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications(
            @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        return ResponseEntity.ok(repository.findByUserIdAndReadFalseOrderByNotifyAtDesc(userId));
    }

    /**
     * Mark notification as read
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable String id,
                                                 @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        Notification n = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        
        // Security check
        if (!n.getUserId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }
        
        n.setRead(true);
        return ResponseEntity.ok(repository.save(n));
    }

    /**
     * Delete notification
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable String id,
                                                  @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        service.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }

    // ================ NOTIFICATION CREATION ================

    /**
     * Create interview reminder for specific application
     */
    @PostMapping("/interview-reminder")
    public ResponseEntity<Notification> createInterviewReminder(
            @Valid @RequestBody InterviewReminderRequest request,
            @PathVariable String applicationId,
            @RequestHeader("Authorization") String authHeader) {
        
        String userId = extractUserId(authHeader);
        
        // Verify user owns the application
        Optional<Application> app = applicationService.getApplication(applicationId, userId);
        
        // Update application with interview date
        app.get().setInterviewDate(request.getInterviewDate());
        applicationService.updateApplication(app.get().getId(), userId, app.get());
        
        // Create notification
        Notification notification = service.createCustomNotification(
                userId, 
                request.getApplicationId(), 
                request.getCustomMessage(), 
                request.getInterviewDate()
        );
        
        return ResponseEntity.ok(notification);
    }

    /**
     * Create assessment deadline reminder for specific application
     */
    @PostMapping("/deadline-reminder")
    public ResponseEntity<Notification> createDeadlineReminder(
            @Valid @RequestBody DeadlineReminderRequest request,
            @PathVariable String applicationId,
            @RequestHeader("Authorization") String authHeader) {
        
        String userId = extractUserId(authHeader);
        
        // Verify user owns the application
        Optional<Application> app = applicationService.getApplication(applicationId, userId);
        
        // Update application with assessment deadline
        app.get().setAssessmentDeadline(request.getAssessmentDeadline());
        applicationService.updateApplication(app.get().getId(), userId, app.get());
        
        // Create notification
        Notification notification = service.createCustomNotification(
                userId, 
                request.getApplicationId(), 
                request.getCustomMessage(), 
                request.getAssessmentDeadline()
        );
        
        return ResponseEntity.ok(notification);
    }

    /**
     * Create custom notification
     
    @PostMapping("/custom")
    public ResponseEntity<Notification> createCustomNotification(
            @Valid @RequestBody CustomNotificationRequest request,
            @RequestHeader("Authorization") String authHeader) {
        
        String userId = extractUserId(authHeader);
        
        Notification n = new Notification();
        n.setUserId(userId);
        n.setApplicationId(request.getApplicationId());
        n.setMessage(request.getMessage());
        n.setNotifyAt(request.getNotifyAt());
        n.setType(request.getType() != null ? request.getType() : Notification.NotificationType.FOLLOW_UP);
        n.setChannel(request.getChannel() != null ? request.getChannel() : Notification.Channel.IN_APP);
        
        return ResponseEntity.ok(service.createCustomNotification(n));
    }

    // ================ NOTIFICATION PREFERENCES ================

    /**
     * Update user notification preferences
     */
    @PutMapping("/preferences")
    public ResponseEntity<User> updateNotificationPreferences(
            @Valid @RequestBody NotificationPreferencesRequest request,
            @RequestHeader("Authorization") String authHeader) {
        
        String userId = extractUserId(authHeader);
        User user = service.updateNotificationPreferences(
                userId, 
                request.isNotificationsEnabled(), 
                request.isEmailEnabled(), 
                request.isInAppEnabled()
        );
        
        return ResponseEntity.ok(user);
    }

    // ================ REAL-TIME NOTIFICATIONS ================

    /**
     * SSE endpoint for live notification updates
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<List<Notification>>> streamNotifications(
            @RequestHeader("Authorization") String authHeader) {
        
        String userId = extractUserId(authHeader);
        
        return Flux.interval(Duration.ofSeconds(10)) // check every 10 sec
                .map(seq -> {
                    List<Notification> unread = repository.findByUserIdAndReadFalse(userId);
                    return ServerSentEvent.<List<Notification>>builder()
                            .id(String.valueOf(seq))
                            .event("notification-update")
                            .data(unread)
                            .build();
                });
    }

    // ================ HELPER METHODS ================

    private String extractUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtUtil.getUserId(token);
    }

    // ================ DTOs ================

    @Data
    static class InterviewReminderRequest {
        @NotBlank(message = "Application ID is required")
        private String applicationId;
        
        @NotNull(message = "Interview date is required")
        private LocalDateTime interviewDate;
        
        private String customMessage;
    }

    @Data
    static class DeadlineReminderRequest {
        @NotBlank(message = "Application ID is required")
        private String applicationId;
        
        @NotNull(message = "Assessment deadline is required")
        private LocalDateTime assessmentDeadline;
        
        private String customMessage;
    }

    @Data
    static class CustomNotificationRequest {
        private String applicationId; // optional
        
        @NotBlank(message = "Message is required")
        private String message;
        
        @NotNull(message = "Notify date/time is required")
        private LocalDateTime notifyAt;
        
        private Notification.NotificationType type;
        private Notification.Channel channel;
    }

    @Data
    static class NotificationPreferencesRequest {
        private boolean notificationsEnabled = true;
        private boolean emailEnabled = true;
        private boolean inAppEnabled = true;
    }
}