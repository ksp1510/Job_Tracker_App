package com.jobtracker.controller;

import com.jobtracker.model.Feedback;
import com.jobtracker.service.FeedbackService;
import com.jobtracker.util.UserContext;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    /**
     * Submit feedback (authenticated or anonymous)
     */
    @PostMapping
    public ResponseEntity<Feedback> submitFeedback(
            @Valid @RequestBody Feedback feedback,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // If user is authenticated, extract userId
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.replace("Bearer ", "");
            String userId = UserContext.getUserId();
            feedback.setUserId(userId);
        }

        Feedback savedFeedback = feedbackService.createFeedback(feedback);
        return ResponseEntity.ok(savedFeedback);
    }

    /**
     * Get user's own feedback
     */
    @GetMapping("/my-feedback")
    public ResponseEntity<List<Feedback>> getMyFeedback(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String userId = UserContext.getUserId();
        return ResponseEntity.ok(feedbackService.getFeedbackByUserId(userId));
    }

    /**
     * Get all feedback (admin endpoint - to be protected later)
     */
    @GetMapping("/all")
    public ResponseEntity<List<Feedback>> getAllFeedback() {
        return ResponseEntity.ok(feedbackService.getAllFeedback());
    }

    /**
     * Get feedback by type
     */
    @GetMapping("/by-type")
    public ResponseEntity<List<Feedback>> getFeedbackByType(@RequestParam String type) {
        return ResponseEntity.ok(feedbackService.getFeedbackByType(type));
    }

    /**
     * Get feedback by status
     */
    @GetMapping("/by-status")
    public ResponseEntity<List<Feedback>> getFeedbackByStatus(@RequestParam String status) {
        return ResponseEntity.ok(feedbackService.getFeedbackByStatus(status));
    }

    /**
     * Get specific feedback
     */
    @GetMapping("/{id}")
    public ResponseEntity<Feedback> getFeedbackById(@PathVariable String id) {
        return feedbackService.getFeedbackById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update feedback (admin response)
     */
    @PutMapping("/{id}")
    public ResponseEntity<Feedback> updateFeedback(
            @PathVariable String id,
            @Valid @RequestBody FeedbackUpdateRequest request) {
        Feedback feedback = new Feedback();
        feedback.setStatus(request.getStatus());
        feedback.setResponse(request.getResponse());
        return ResponseEntity.ok(feedbackService.updateFeedback(id, feedback));
    }

    /**
     * Delete feedback
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFeedback(@PathVariable String id) {
        feedbackService.deleteFeedback(id);
        return ResponseEntity.noContent().build();
    }

    // DTO for update request
    @Data
    static class FeedbackUpdateRequest {
        private String status;
        private String response;
    }
}
