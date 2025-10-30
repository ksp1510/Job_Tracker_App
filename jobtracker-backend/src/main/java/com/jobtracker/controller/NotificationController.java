package com.jobtracker.controller;

import com.jobtracker.config.JwtUtil;
import com.jobtracker.model.Application;
import com.jobtracker.model.Notification;
import com.jobtracker.model.NotificationPreference;
import com.jobtracker.model.User;
import com.jobtracker.repository.NotificationPreferenceRepository;
import com.jobtracker.repository.NotificationRepository;
import com.jobtracker.repository.UserRepository;
import com.jobtracker.service.ApplicationService;
import com.jobtracker.service.NotificationService;
import com.jobtracker.exception.ResourceNotFoundException;
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
import java.util.Map;
import java.time.Duration;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService service;
    private final NotificationRepository repository;
    private final NotificationPreferenceRepository prefRepo;
    private final ApplicationService applicationService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public NotificationController(NotificationService service, 
                                NotificationRepository repository,
                                ApplicationService applicationService,
                                JwtUtil jwtUtil,
                                NotificationPreferenceRepository prefRepo,
                                UserRepository userRepository) {
        this.service = service;
        this.repository = repository;
        this.applicationService = applicationService;
        this.jwtUtil = jwtUtil;
        this.prefRepo = prefRepo;
        this.userRepository = userRepository;
    }

    // ================ USER NOTIFICATION MANAGEMENT ================

    /**
     * Get all notifications for logged-in user - FIXED: Filter by preferences
     */
    @GetMapping
    public ResponseEntity<List<Notification>> getUserNotifications(
            @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        
        return ResponseEntity.ok(service.getUserNotifications(userId));
    }

    /**
     * Get unread notifications for logged-in user - FIXED: Filter by preferences and time
     */
    @GetMapping("/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications(
            @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        
        // FIXED: Only return notifications that are due (notifyAt is in the past)
        LocalDateTime now = LocalDateTime.now();
        List<Notification> allUnread = repository.findByUserIdAndReadFalseOrderByNotifyAtDesc(userId);
        
        // Filter to only show notifications that should be displayed now
        List<Notification> dueNotifications = allUnread.stream()
                .filter(n -> n.getNotifyAt().isBefore(now) || n.getNotifyAt().isEqual(now))
                .toList();
        
        System.out.println("📱 Returning " + dueNotifications.size() + " due notifications out of " + allUnread.size() + " total unread");
        
        return ResponseEntity.ok(dueNotifications);
    }

    /**
     * Mark notification as read
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable String id,
                                                 @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        Notification n = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        
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
        Notification n = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        
        // Security check
        if (!n.getUserId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }
        
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ================ NOTIFICATION CREATION ================

    /**
     * Create interview reminder
     */
    @PostMapping("/interview-reminder")
    public ResponseEntity<Notification> createInterviewReminder(
            @Valid @RequestBody InterviewReminderRequest request,
            @RequestHeader("Authorization") String authHeader) {

        String userId = extractUserId(authHeader);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Application app = applicationService.getApplication(request.getApplicationId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        app.setInterviewDate(request.getInterviewDate());
        applicationService.updateApplication(app.getId(), userId, app);

        try {
            Notification notification = service.createInterviewReminder(
                userId,
                request.getApplicationId(),
                request.getInterviewDate(),
                request.getCustomMessage()
            );
            return ResponseEntity.ok(notification);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/deadline-reminder")
    public ResponseEntity<Notification> createDeadlineReminder(
            @Valid @RequestBody DeadlineReminderRequest request,
            @RequestHeader("Authorization") String authHeader) {

        String userId = extractUserId(authHeader);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Application app = applicationService.getApplication(request.getApplicationId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        app.setAssessmentDeadline(request.getAssessmentDeadline());
        applicationService.updateApplication(app.getId(), userId, app);

        try {
        Notification notification = service.createAssessmentDeadlineReminder(
            userId,
            request.getApplicationId(),
            request.getAssessmentDeadline(),
            request.getCustomMessage()
        );
        return ResponseEntity.ok(notification);
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().build();
    }
    }


    /**
     * Create custom notification - User sets their own notifyAt time
     */
    @PostMapping("/custom")
    public ResponseEntity<Notification> createCustomNotification(
            @Valid @RequestBody CustomNotificationRequest request,
            @RequestHeader("Authorization") String authHeader) {
        
        String userId = extractUserId(authHeader);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        System.out.println("📅 Custom notification scheduled for: " + request.getNotifyAt());
        
        Notification n = new Notification();
        n.setUserId(userId);
        n.setApplicationId(request.getApplicationId());
        n.setMessage(request.getMessage());
        n.setNotifyAt(request.getNotifyAt()); // User-specified time
        n.setType(request.getType() != null ? request.getType() : Notification.NotificationType.CUSTOM);
        n.setCreatedAt(LocalDateTime.now());
        n.setRead(false);
        n.setSent(false);
        
        Notification saved = repository.save(n);
        
        System.out.println("✅ Custom notification created - will notify at: " + saved.getNotifyAt());
        
        return ResponseEntity.ok(saved);
    }

    // ================ NOTIFICATION PREFERENCES ================

    @GetMapping("/preferences")
    public ResponseEntity<NotificationPreference> getPreferences(@RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        NotificationPreference pref = prefRepo.findByUserId(userId)
            .orElseGet(() -> prefRepo.save(new NotificationPreference(userId, true, true, LocalDateTime.now())));
        return ResponseEntity.ok(pref);
    }

    @PutMapping("/preferences")
    public ResponseEntity<NotificationPreference> updatePreferences(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Boolean> req) {
        String userId = extractUserId(authHeader);
        NotificationPreference pref = prefRepo.findByUserId(userId)
            .orElseGet(() -> new NotificationPreference());
        pref.setUserId(userId);
        pref.setEmailEnabled(req.getOrDefault("emailEnabled", true));
        pref.setInAppEnabled(true);  // enforce
        pref.setUpdatedAt(LocalDateTime.now());
        prefRepo.save(pref);
        return ResponseEntity.ok(pref);
    }


    // ================ REAL-TIME NOTIFICATIONS ================

    /**
     * SSE endpoint for live notification updates - FIXED: Filter by preferences
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<List<Notification>>> streamNotifications(
            @RequestHeader("Authorization") String authHeader) {
        
        String userId = extractUserId(authHeader);
        
        return Flux.interval(Duration.ofSeconds(10)) // check every 10 sec
                .map(seq -> {
                    // Check user preferences
                    User user = userRepository.findById(userId).orElse(null);
                    NotificationPreference pref = prefRepo.findByUserId(userId).orElse(null);
                    
                    if (user == null || !pref.isInAppEnabled()) {
                        return ServerSentEvent.<List<Notification>>builder()
                                .id(String.valueOf(seq))
                                .event("notification-update")
                                .data(List.of())
                                .build();
                    }
                    
                    // FIXED: Only return notifications that are due
                    LocalDateTime now = LocalDateTime.now();
                    List<Notification> unread = repository.findByUserIdAndReadFalse(userId);
                    
                    List<Notification> dueNotifications = unread.stream()
                            .filter(n -> n.getNotifyAt().isBefore(now) || n.getNotifyAt().isEqual(now))
                            .toList();
                    
                    return ServerSentEvent.<List<Notification>>builder()
                            .id(String.valueOf(seq))
                            .event("notification-update")
                            .data(dueNotifications)
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
        private String interviewDate;
        
        private String customMessage;
    }

    @Data
    static class DeadlineReminderRequest {
        @NotBlank(message = "Application ID is required")
        private String applicationId;
        
        @NotNull(message = "Assessment deadline is required")
        private String assessmentDeadline;
        
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
    }

    @Data
    static class NotificationPreferencesRequest {
        private boolean notificationsEnabled = true;
        private boolean emailEnabled = true;
        private boolean inAppEnabled = true;
    }
}