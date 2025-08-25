package com.jobtracker.controller;

import com.jobtracker.model.Notification;
import com.jobtracker.repository.NotificationRepository;
import com.jobtracker.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;

import java.util.List;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService service;
    private final NotificationRepository repository;

    public NotificationController(NotificationService service, NotificationRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<Notification> create(@RequestBody Notification n,
                                            @RequestParam(required = false) String reminderTime) {
        if (reminderTime != null) {
            try {
                n.setNotifyAt(Instant.parse(reminderTime));
            } catch (DateTimeParseException e) {
                throw new RuntimeException("Invalid reminderTime format. Use ISO-8601 (e.g., 2025-08-26T13:30:00Z).");
            }
        }
        return ResponseEntity.ok(service.createNotification(n));
    }



    @GetMapping("/{userId}/all")
    public ResponseEntity<List<Notification>> getUserNotifications(@PathVariable String userId) {
        return ResponseEntity.ok(service.getUserNotifications(userId));
    }

    // Get unread notifications for a user
    @GetMapping("/{userId}/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications(@PathVariable String userId) {
        return ResponseEntity.ok(repository.findByUserIdAndReadFalse(userId));
    }

    // Mark a notification as read
    @PatchMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable String id) {
        Notification n = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        n.setRead(true);
        return ResponseEntity.ok(repository.save(n));
    }

    // Delete a notification
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable String id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // SSE endpoint for live updates
    @GetMapping(value = "/stream/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<List<Notification>>> streamNotifications(@PathVariable String userId) {
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
}
