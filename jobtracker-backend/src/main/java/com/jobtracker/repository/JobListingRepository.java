// Job Listing Repository
package com.jobtracker.repository;

import com.jobtracker.model.JobListing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.Instant;
import java.util.List;

public interface JobListingRepository extends MongoRepository<JobListing, String> {
    
    Page<JobListing> findByIsActiveTrue(Pageable pageable);
    Page<JobListing> findByIsActiveTrueAndTitleContainingIgnoreCase(String title, Pageable pageable);
    Page<JobListing> findByIsActiveTrueAndLocationContainingIgnoreCase(String location, Pageable pageable);
    Page<JobListing> findByIsActiveTrueAndTitleContainingIgnoreCaseAndLocationContainingIgnoreCase(String title, String location, Pageable pageable);
    List<JobListing> findByIsActiveTrueAndLocationNear(Point location, Distance distance);
    List<JobListing> findByFetchedAtBefore(Instant cutoff);
    Page<JobListing> findByIsActiveTrueAndTitleRegex(String string, Pageable pageable);
    Page<JobListing> findByIsActiveTrueAndTitleRegexAndLocationRegex(String string, String string2, Pageable pageable);
}