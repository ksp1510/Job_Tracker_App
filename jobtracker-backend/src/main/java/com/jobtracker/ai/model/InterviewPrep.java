package com.jobtracker.ai.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "interview_prep")
public class InterviewPrep {

    @Id
    private String id;

    private String userId;

    private String question;

    private String answer; // user's answer

    private String aiFeedback; // AI-generated feedback

    private String category; // TECHNICAL, BEHAVIORAL, COMPANY_SPECIFIC

    private String difficulty; // EASY, MEDIUM, HARD

    private Instant createdAt = Instant.now();
    
    private Instant lastPracticed;
    
    private int practiceCount = 0;
    
    private boolean isBookmarked = false;
}
