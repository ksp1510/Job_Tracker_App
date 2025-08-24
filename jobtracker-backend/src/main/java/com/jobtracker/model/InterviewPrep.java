package com.jobtracker.model;

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

    private String answer; // user’s answer

    private String aiFeedback; // optional, filled when AI layer adds feedback

    private Instant createdAt = Instant.now();
}
