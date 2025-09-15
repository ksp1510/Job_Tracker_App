// Saved Job Repository
package com.jobtracker.repository;

import com.jobtracker.model.SavedJob;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface SavedJobRepository extends MongoRepository<SavedJob, String> {
    List<SavedJob> findByUserIdOrderBySavedAtDesc(String userId);
    Optional<SavedJob> findByUserIdAndJobListingId(String userId, String jobListingId);
    boolean existsByUserIdAndJobListingId(String userId, String jobListingId);
}