// AI Service
package com.jobtracker.ai.service;

import com.jobtracker.ai.model.InterviewPrep;
import com.jobtracker.ai.repository.InterviewPrepRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
public class InterviewPrepService {

    private final InterviewPrepRepository repository;
    
    public InterviewPrepService(InterviewPrepRepository repository) {
        this.repository = repository;
    }

    public InterviewPrep saveQuestion(String userId, String question, String answer, String category) {
        InterviewPrep prep = new InterviewPrep();
        prep.setUserId(userId);
        prep.setQuestion(question);
        prep.setAnswer(answer);
        prep.setCategory(category);
        prep.setCreatedAt(Instant.now());
        
        return repository.save(prep);
    }

    public List<InterviewPrep> getUserQuestions(String userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<InterviewPrep> getQuestionsByCategory(String userId, String category) {
        return repository.findByUserIdAndCategoryOrderByCreatedAtDesc(userId, category);
    }

    public InterviewPrep updateWithAiFeedback(String id, String aiFeedback) {
        InterviewPrep prep = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Interview prep not found"));
        
        prep.setAiFeedback(aiFeedback);
        prep.setLastPracticed(Instant.now());
        prep.setPracticeCount(prep.getPracticeCount() + 1);
        
        return repository.save(prep);
    }

    public void toggleBookmark(String id, String userId) {
        InterviewPrep prep = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Interview prep not found"));
        
        if (!prep.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        prep.setBookmarked(!prep.isBookmarked());
        repository.save(prep);
    }

    // TODO: Integrate with AI service for generating feedback
    public String generateAiFeedback(String question, String userAnswer) {
        // This would integrate with ChatGPT, Claude, or other AI services
        // For now, return a placeholder
        return "AI feedback will be generated here based on the question and answer.";
    }
}