package com.jobtracker.repository;

import com.jobtracker.model.Feedback;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface FeedbackRepository extends MongoRepository<Feedback, String> {
    List<Feedback> findByUserId(String userId);
    List<Feedback> findByType(String type);
    List<Feedback> findByStatus(String status);
    List<Feedback> findByUserIdOrderByCreatedAtDesc(String userId);
}
