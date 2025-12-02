// AI Controller
package com.jobtracker.ai.controller;

import com.jobtracker.ai.model.InterviewPrep;
import com.jobtracker.ai.service.InterviewPrepService;
import com.jobtracker.util.UserContext;
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

    public InterviewPrepController(InterviewPrepService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<InterviewPrep> saveQuestion(
            @Valid @RequestBody InterviewPrepRequest request) {
        
        String userId = UserContext.getUserId();
        InterviewPrep prep = service.saveQuestion(
                userId, 
                request.getQuestion(), 
                request.getAnswer(), 
                request.getCategory()
        );
        
        return ResponseEntity.ok(prep);
    }

    @GetMapping
    public ResponseEntity<List<InterviewPrep>> getUserQuestions() {
        
        String userId = UserContext.getUserId();
        return ResponseEntity.ok(service.getUserQuestions(userId));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<InterviewPrep>> getQuestionsByCategory(
            @PathVariable String category) {
        
        String userId = UserContext.getUserId();
        return ResponseEntity.ok(service.getQuestionsByCategory(userId, category));
    }

    @PostMapping("/{id}/feedback")
    public ResponseEntity<InterviewPrep> generateFeedback(
            @PathVariable String id) {
        
        // Generate AI feedback and update the record
        String feedback = service.generateAiFeedback("", ""); // TODO: Get actual question/answer
        InterviewPrep updated = service.updateWithAiFeedback(id, feedback);
        
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/bookmark")
    public ResponseEntity<Void> toggleBookmark(
            @PathVariable String id) {
        
        String userId = UserContext.getUserId();
        service.toggleBookmark(id, userId);
        
        return ResponseEntity.ok().build();
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