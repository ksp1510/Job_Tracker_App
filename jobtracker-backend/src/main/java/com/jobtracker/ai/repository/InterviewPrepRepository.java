// AI Repository
package com.jobtracker.ai.repository;

import com.jobtracker.ai.model.InterviewPrep;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface InterviewPrepRepository extends MongoRepository<InterviewPrep, String> {
    List<InterviewPrep> findByUserIdOrderByCreatedAtDesc(String userId);
    List<InterviewPrep> findByUserIdAndCategoryOrderByCreatedAtDesc(String userId, String category);
    List<InterviewPrep> findByUserIdAndIsBookmarkedTrue(String userId);
}