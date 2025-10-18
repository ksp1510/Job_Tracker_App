// Job Search Repository
package com.jobtracker.repository;

import com.jobtracker.model.JobSearch;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface JobSearchRepository extends MongoRepository<JobSearch, String> {

    List<JobSearch> findByUserIdOrderBySearchedAtDesc(String userId, String query, String location);
    
    List<JobSearch> findTop10ByUserIdOrderBySearchedAtDesc(String userId);
}