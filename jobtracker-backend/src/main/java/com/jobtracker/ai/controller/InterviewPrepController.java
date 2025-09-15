// AI Controller
package com.jobtracker.ai.controller;

import com.jobtracker.ai.model.InterviewPrep;
import com.jobtracker.ai.service.InterviewPrepService;
import com.jobtracker.config.JwtUtil;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

@RestController
@RequestMapping("/ai/interview-prep")
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
public class InterviewPrepController {

    private final InterviewPrepService service;
    private final JwtUtil jwtUtil;

    public InterviewPrepController(InterviewPrepService service, JwtUtil jwtUtil) {
        this.service = service;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping
    public ResponseEntity<InterviewPrep> saveQuestion(
            @Valid @RequestBody InterviewPrepRequest request,
            @RequestHeader("Authorization") String authHeader) {
        
        String userId = extractUserId(authHeader);
        InterviewPrep prep = service.saveQuestion(
                userId, 
                request.getQuestion(), 
                request.getAnswer(), 
                request.getCategory()
        );
        
        return ResponseEntity.ok(prep);
    }

    @GetMapping
    public ResponseEntity<List<InterviewPrep>> getUserQuestions(
            @RequestHeader("Authorization") String authHeader) {
        
        String userId = extractUserId(authHeader);
        return ResponseEntity.ok(service.getUserQuestions(userId));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<InterviewPrep>> getQuestionsByCategory(
            @PathVariable String category,
            @RequestHeader("Authorization") String authHeader) {
        
        String userId = extractUserId(authHeader);
        return ResponseEntity.ok(service.getQuestionsByCategory(userId, category));
    }

    @PostMapping("/{id}/feedback")
    public ResponseEntity<InterviewPrep> generateFeedback(
            @PathVariable String id,
            @RequestHeader("Authorization") String authHeader) {
        
        // Generate AI feedback and update the record
        String feedback = service.generateAiFeedback("", ""); // TODO: Get actual question/answer
        InterviewPrep updated = service.updateWithAiFeedback(id, feedback);
        
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/bookmark")
    public ResponseEntity<Void> toggleBookmark(
            @PathVariable String id,
            @RequestHeader("Authorization") String authHeader) {
        
        String userId = extractUserId(authHeader);
        service.toggleBookmark(id, userId);
        
        return ResponseEntity.ok().build();
    }

    private String extractUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtUtil.getUserId(token);
    }

    @Data
    static class InterviewPrepRequest {
        @NotBlank(message = "Question is required")
        private String question;
        
        @NotBlank(message = "Answer is required")
        private String answer;
        
        private String category = "GENERAL";
    }
}