package com.jobtracker.service;

import com.jobtracker.model.Feedback;
import com.jobtracker.repository.FeedbackRepository;
import com.jobtracker.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;

    public FeedbackService(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    /**
     * Create new feedback
     */
    public Feedback createFeedback(Feedback feedback) {
        feedback.setCreatedAt(Instant.now());
        feedback.setStatus("NEW");
        return feedbackRepository.save(feedback);
    }

    /**
     * Get all feedback (admin only)
     */
    public List<Feedback> getAllFeedback() {
        return feedbackRepository.findAll();
    }

    /**
     * Get feedback by user ID
     */
    public List<Feedback> getFeedbackByUserId(String userId) {
        return feedbackRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Get feedback by type
     */
    public List<Feedback> getFeedbackByType(String type) {
        return feedbackRepository.findByType(type);
    }

    /**
     * Get feedback by status
     */
    public List<Feedback> getFeedbackByStatus(String status) {
        return feedbackRepository.findByStatus(status);
    }

    /**
     * Get specific feedback by ID
     */
    public Optional<Feedback> getFeedbackById(String id) {
        return feedbackRepository.findById(id);
    }

    /**
     * Update feedback (admin only - for adding response)
     */
    public Feedback updateFeedback(String id, Feedback feedbackDetails) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found"));

        if (feedbackDetails.getStatus() != null) {
            feedback.setStatus(feedbackDetails.getStatus());
        }
        if (feedbackDetails.getResponse() != null) {
            feedback.setResponse(feedbackDetails.getResponse());
        }
        feedback.setUpdatedAt(Instant.now());

        return feedbackRepository.save(feedback);
    }

    /**
     * Delete feedback
     */
    public void deleteFeedback(String id) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found"));
        feedbackRepository.delete(feedback);
    }
}
