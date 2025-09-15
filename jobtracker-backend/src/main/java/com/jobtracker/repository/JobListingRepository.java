// Job Listing Repository
package com.jobtracker.repository;

import com.jobtracker.model.JobListing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.time.Instant;
import java.util.List;

public interface JobListingRepository extends MongoRepository<JobListing, String> {
    
    @Query("{ 'isActive': true }")
    Page<JobListing> findActiveJobs(Pageable pageable);
    
    @Query("{ 'isActive': true, 'title': { $regex: ?0, $options: 'i' } }")
    Page<JobListing> findByTitleContainingIgnoreCase(String title, Pageable pageable);
    
    @Query("{ 'isActive': true, 'location': { $regex: ?0, $options: 'i' } }")
    Page<JobListing> findByLocationContainingIgnoreCase(String location, Pageable pageable);
    
    @Query("{ 'isActive': true, 'company': { $regex: ?0, $options: 'i' } }")
    Page<JobListing> findByCompanyContainingIgnoreCase(String company, Pageable pageable);
    
    @Query("{ 'isActive': true, 'skills': { $in: ?0 } }")
    Page<JobListing> findBySkillsIn(List<String> skills, Pageable pageable);
    
    @Query("{ 'isActive': true, 'salary': { $gte: ?0, $lte: ?1 } }")
    Page<JobListing> findBySalaryBetween(Double minSalary, Double maxSalary, Pageable pageable);
    
    List<JobListing> findByFetchedAtBefore(Instant cutoffTime);
}